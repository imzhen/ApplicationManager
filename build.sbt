name := "ApplicationManager"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= {
  val akkaVersion = "2.4.16"
  val akkaHttpVersion = "10.0.3"
  Seq (
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.github.nscala-time" %% "nscala-time" % "2.16.0"
  )
}