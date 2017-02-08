package backend.scrape

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import org.htmlcleaner.HtmlCleaner
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase, Observer}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.xml._

/**
  * Created by Elliott on 2/5/17.
  */

object Scraper {

  case class URL(url: String)

}

class Scraper extends Actor with ActorLogging with Crawler {

  import Scraper._

  val pageParser: ActorRef = context.actorOf(PageParser.props, PageParser.name)

  def receive = {
    case URL(url) => crawl(url, parse)
  }

  def parse(e: HttpResponse) = Unmarshal(e.entity).to[String] onComplete {
    case Success(s) => pageParser ! s
    case Failure(t) => println(t)
  }
}

object PageParser {

  def props = Props(new PageParser)
  def name = "pageParser"

}

class PageParser extends Actor with ActorLogging with RecordToDocument {

  val hc = new HtmlCleaner()
  val dBWriter: ActorRef = context.actorOf(DBWriter.props, DBWriter.name)

  def receive = {
    case s: String => parse(s)
  }

  def parse(s: String) = {
    val validHtml = s"<html>${hc.getInnerHtml(hc.clean(s))}</html>"
    val nodes = XML.loadString(validHtml)
    val records = (nodes \\ "table")
      .filter(e => (e \ "@id").text == "threadlisttableid")
      .flatMap(x => x \ "tbody")
      .filter(e => (e \ "@id").text.startsWith("normalthread"))
    val ids = (records \\ "@id").map(_.toString)
    val spans = records
      .flatMap(x => x \\ "span")
      .filter(e => (e \ "@style").text == "margin-top: 3px")
    val nextPage = (nodes \\ "a").filter(e => (e \ "@class").text == "nxt")

    if (nextPage.length > 0) {
      val nextPageUrl = """http://www.1point3acres.com""".r.replaceAllIn((nextPage.head \ "@href").text, "")
//      context.system.scheduler.scheduleOnce(3.seconds, sender, Scraper.URL(nextPageUrl))
    }

    dBWriter ! (ids zip spans).flatMap(s => recordToDocument(s._1, s._2))
  }
}

object DBWriter {

  case object InsertDone

  def props = Props(new DBWriter)
  def name = "dBWriter"

}

class DBWriter extends Actor with ActorLogging with MongoDBConf {

  import DBWriter._

  var counter = 0

  def receive = {
    case doc: List[Document] =>
      counter += 1
      collection.insertMany(doc).subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = ()
        override def onError(e: Throwable): Unit = println("Failed")
        override def onComplete(): Unit = self ! InsertDone
      })
    case InsertDone =>
      counter -= 1
      counter match {
        case 0 => mongoClient.close()
        case _ =>
      }
  }

}


trait Crawler {

  this: Actor =>

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  implicit val executionContext: ExecutionContext = context.dispatcher

  def crawl(uri: String, successAction: HttpResponse => Unit) = {

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http(context.system).outgoingConnection("www.1point3acres.com")
    val request: HttpRequest = HttpRequest(uri = uri)
    val response: Future[HttpResponse] = Source.single(request)
      .via(connectionFlow)
      .runWith(Sink.head)

    response onComplete {
      case Success(e) => successAction(e)
      case Failure(t) =>
        println(t)
        self ! Scraper.URL(uri)
    }
  }
}

trait RecordToDocument {

  def recordToDocument(id: String, node: Node): Option[Document] = {
    val fontGroups: NodeSeq = node \\ "font"
    if (fontGroups.length == 13) {
      Some(Document(
        "threadId" -> "[0-9]+".r.findFirstIn(id).get.toInt,
        "offer" -> Document(
          "academicYear" -> fontGroups.head.text.tail,
          "degree" -> fontGroups(1).text,
          "offerType" -> fontGroups(2).text.dropRight(1),
          "offerMajor" -> Document(
            "major" -> fontGroups(3).text,
            "university" -> fontGroups(4).text
          )
        ),
        "reportTime" -> fontGroups(5).text,
        "personInfo" -> Document(
          "toefl" -> fontGroups(6).text.drop(3),
          "gre" -> fontGroups(7).text.drop(3),
          "bachelorMajor" -> Document(
            "major" -> fontGroups(8).text,
            "gpa" -> fontGroups(9).text.slice(1, fontGroups(9).text.length-1),
            "university" -> fontGroups(10).text
          ),
          "mastersMajor" -> Document(
            "major" -> fontGroups(11).text,
            "university" -> fontGroups(12).text
          )
        )
      ))
    } else {
      None
    }
  }

}

trait MongoDBConf {
  val mongoClient: MongoClient = MongoClient()
  val database: MongoDatabase = mongoClient.getDatabase("AppManager")
  val collection: MongoCollection[Document] = database.getCollection("Application")
}
