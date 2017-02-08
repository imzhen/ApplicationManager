package backend.scrape

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
  * Created by Elliott on 2/5/17.
  */

object Main extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("Scraper")
  val scraper = system.actorOf(Props[Scraper])
  val initialUri = "/bbs/forum-82-1.html"
  scraper ! Scraper.URL(initialUri)
}
