package backend.scrape

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization._
import org.htmlcleaner.HtmlCleaner

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.xml._


object Scraper {

  case class URL(url: String)

  def makeUri(i: Int) = s"/bbs/forum-82-$i.html"

}

class Scraper extends Actor with ActorLogging with Crawler {

  import Scraper._

  val maxPage = 925

  val kafkaProducer: ActorRef = context.actorOf(KafkaProducer.props, KafkaProducer.name)

  def receive = {
    case i: Int if i < maxPage =>
      crawl(makeUri(i), parse)
//      if (i < maxPage - 1) {
//        context.system.scheduler.scheduleOnce(3.seconds, self, i + 1)
//      }
  }

  def parse(e: HttpResponse) = Unmarshal(e.entity).to[String] onComplete {
    case Success(s) => kafkaProducer ! s
    case Failure(t) => println(t)
  }
}

object KafkaProducer {

  def props = Props(new KafkaProducer)

  def name = "kafkaProducer"

}

class KafkaProducer extends Actor with ActorLogging with RecordExtractor with KafkaSettings {

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  implicit val executionContext: ExecutionContext = context.dispatcher

  val producerSettings = ProducerSettings(context.system, new StringSerializer, new JsonSerializer[Record])
    .withBootstrapServers(brokers)
  var counter = 1

  def receive = {
    case s: String =>
      val records: Seq[Record] = recordExtractor(s)
      val producerGraph = Source(records.toVector)
        .map { msg =>
          val partition = 0
          new ProducerRecord[String, Record](topic, partition, null, msg)
        }
        .to(Producer.plainSink(producerSettings))
      producerGraph.run()
  }

}


object ScraperProducer extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("Scraper")
  val scraper = system.actorOf(Props[Scraper])
  scraper ! 1
}


trait Crawler {

  this: Actor =>

  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  implicit val executionContext: ExecutionContext = context.dispatcher

  def crawl(uri: String, successAction: HttpResponse => Unit) = {

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http(context.system).outgoingConnection("www.1point3acres.com")
    val request: HttpRequest = HttpRequest(uri = uri)
    val response: RunnableGraph[Future[HttpResponse]] = Source.single(request)
      .via(connectionFlow)
      .toMat(Sink.head)(Keep.right)

    response.run() onComplete {
      case Success(e) => successAction(e)
      case Failure(t) =>
        println(t)
        self ! Scraper.URL(uri)
    }
  }
}


trait RecordExtractor {
  val hc = new HtmlCleaner()

  def recordExtractor(s: String): Seq[Record] = {
    val validHtml = s"<html>${hc.getInnerHtml(hc.clean(s))}</html>"
    val nodes = XML.loadString(validHtml)
    val records = (nodes \\ "table")
      .filter(e => (e \ "@id").text == "threadlisttableid")
      .flatMap(x => x \ "tbody")
      .filter(e => (e \ "@id").text.startsWith("normalthread"))
      .filter(e => (e \\ "span").filter(e1 => (e1 \ "@style").text == "margin-top: 3px").length != 0)
    val links = (records \\ "a")
      .filter(e => (e \ "@class").text == "s xst")
      .map(e => (e \ "@href").text)
    val ids = links.map(s => """[0-9]{4,}""".r.findFirstIn(s).get)
    val spans = records
      .flatMap(x => x \\ "span")
      .filter(e => (e \ "@style").text == "margin-top: 3px")
    val fontGroups = spans.map(_ \\ "font")

    (ids zip fontGroups).collect {
      case (id, fontGroup) if fontGroup.length == 13 =>
        Record("[0-9]+".r.findFirstIn(id).get.toInt, fontGroup.head.text.tail, fontGroup(1).text,
          fontGroup(2).text.dropRight(1), fontGroup(3).text, fontGroup(4).text, fontGroup(5).text,
          fontGroup(6).text.drop(3), fontGroup(7).text.drop(3), fontGroup(8).text,
          fontGroup(9).text.slice(1, fontGroup(9).text.length - 1), fontGroup(10).text,
          fontGroup(11).text, fontGroup(12).text)
    }
  }
}

