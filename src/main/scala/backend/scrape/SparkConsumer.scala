package backend.scrape

import com.datastax.spark.connector.SomeColumns
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector._
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import play.api.libs.json.Json


object SparkConsumer extends App with KafkaSettings {

  val spark = SparkSession
    .builder
    .master("local[2]")
    .appName("scraperConsumer")
    .getOrCreate
  spark.sparkContext.setLogLevel("WARN")

  val ssc = new StreamingContext(spark.sparkContext, Seconds(5))
  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> brokers,
    "key.deserializer" -> classOf[ByteArrayDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> group
  )
  val topicsSet: Set[String] = topic.split(",").toSet
  val messages = KafkaUtils.createDirectStream(ssc, PreferConsistent, Subscribe[Array[Byte], String](topicsSet, kafkaParams))
  val lines = messages.map(_.value).map(e => Json.parse(e).as[Record])

  CassandraConnector(spark.sparkContext).withSessionDo{session =>
    session.execute("DROP keyspace IF EXISTS AppManager")
    session.execute("CREATE keyspace AppManager WITH replication={'class': 'SimpleStrategy', 'replication_factor': 1}")
    session.execute(
      """CREATE TABLE AppManager.raw (id INT PRIMARY KEY, "offerAcademicYear" VARCHAR, "offerDegree" VARCHAR,
        | "offerType" VARCHAR, "offerMajor" VARCHAR, "offerUniversity" VARCHAR, "reportTime" VARCHAR, "toefl" VARCHAR,
        | "gre" VARCHAR, "bachelorMajor" VARCHAR, "bachelorGPA" VARCHAR, "bachelorUniversity" VARCHAR,
        | "masterMajor" VARCHAR, "masterUniversity" VARCHAR)""".stripMargin)
  }

  lines.foreachRDD{rdd =>
    if (rdd.toLocalIterator.nonEmpty) {
      rdd.saveToCassandra("appmanager", "raw",
        SomeColumns("id", "offerAcademicYear", "offerDegree", "offerType", "offerMajor", "offerUniversity", "reportTime",
          "toefl", "gre", "bachelorMajor", "bachelorGPA", "bachelorUniversity", "masterMajor", "masterUniversity"))
    }
  }

  ssc.start
  ssc.awaitTermination

}
