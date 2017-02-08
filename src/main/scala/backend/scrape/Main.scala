package backend.scrape

import akka.actor.{ActorSystem, Props}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Elliott on 2/5/17.
  */

object Main extends App {
  val system = ActorSystem("Scraper")
  val scraper = system.actorOf(Props[Scraper])
  val initialUri = "/bbs/forum.php?mod=forumdisplay&fid=82&sortid=164&%1=&sortid=164&page=1"
  scraper ! Scraper.URL(initialUri)
  system.scheduler.scheduleOnce(50.seconds) {
    system.terminate
  }
}
