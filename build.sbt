name := "ApplicationManager"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions += "-feature"

val akkaVersion = "2.4.17"
val akkaHttpVersion = "10.0.3"
val playVersion = "2.6.0-M3"
val sparkVersion = "2.1.0"
val kafkaVersion = "0.10.2.0"
val akkaStreamKafkaVersion = "0.14"
val sparkCassandraVersion = "2.0.0"

libraryDependencies ++= {
  Seq (
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.play" %% "play" % playVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" % akkaStreamKafkaVersion,
    "com.github.nscala-time" %% "nscala-time" % "2.16.0",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.2",
    "org.mongodb.scala" %% "mongo-scala-driver" % "1.2.1",
    "ch.qos.logback" % "logback-classic" % "1.2.2",
    "org.apache.kafka" %% "kafka" % kafkaVersion,
    "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.apache.spark" %% "spark-streaming" % sparkVersion,
    "com.datastax.spark" %% "spark-cassandra-connector" % sparkCassandraVersion,
    "com.typesafe" % "config" % "1.3.1"
  )
}.map(_.exclude("org.slf4j","slf4j-log4j12"))

dependencyOverrides ++= Set("com.fasterxml.jackson.core" % "jackson-databind" % "2.6.0")