package backend.rest

import java.net.URL

import akka.actor.{Actor, Props}
import com.github.nscala_time.time.Imports._

/**
  * Created by Elliott on 2/5/17.
  */

object CurrentStatus extends Enumeration {
  type CurrentStatus = Value
  val Saved, Applied, Pending, Offered, Declined = Value
}

object ApplicationRouter {
  import CurrentStatus._
  case class Job(name: String, tag: String, jobWebsite: URL, description: String)
  case class Company(name: String, dashboard: URL)

  case class Application(time: DateTime, company: Company, status: CurrentStatus, job: Job)
//  It should be a filter to get a list of applications
  case class GetApplications()

  case class CreateApplication(time: DateTime, company: Company, status: CurrentStatus, job: Job, hashcode: String)
  case class UpdateApplication(time: DateTime, status: CurrentStatus)
}

class ApplicationRouter extends Actor {

  import ApplicationRouter._
  import ApplicationWriter._
  import CurrentStatus._

  def createApplication(application: Application) = context.actorOf(props(application), name(application))

  def receive = {
    case CreateApplication(time: DateTime, company: Company, status: CurrentStatus, job: Job, hashcode: String) =>
      ???
  }
}
