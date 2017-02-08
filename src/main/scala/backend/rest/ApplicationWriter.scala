package backend.rest

import akka.actor.{Actor, Props}
import ApplicationRouter._

/**
  * Created by Elliott on 2/5/17.
  */

object ApplicationWriter {

  def props(application: Application) = Props(new ApplicationWriter(application))
}

class ApplicationWriter(application: Application) extends Actor{
  def receive = {
    ???
  }
}
