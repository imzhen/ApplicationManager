package backend.scrape

import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}
import org.mongodb.scala.Document
import play.api.libs.json.{Json, Reads, Writes}

import scala.language.implicitConversions


case class Record(id: Int, offerAcademicYear: String, offerDegree: String,
                  offerType: String, offerMajor: String, offerUniversity: String,
                  reportTime: String, toefl: String, gre: String, bachelorMajor: String,
                  bachelorGPA: String, bachelorUniversity: String, masterMajor: String,
                  masterUniversity: String)

object Record {

  implicit val recordFormat = Json.format[Record]

  implicit def recordDocument(record: Record): Document = {
    Document(Json.stringify(Json.toJson(record)))
  }
}

class JsonSerializer[A: Writes] extends Serializer[A] {

  val stringSerializer = new StringSerializer

  override def configure(configs: java.util.Map[String, _], isKey: Boolean) =
    stringSerializer.configure(configs, isKey)

  override def serialize(topic: String, data: A) =
    stringSerializer.serialize(topic, Json.stringify(Json.toJson(data)))

  override def close() =
    stringSerializer.close()

}

class JsonDeserializer[A: Reads] extends Deserializer[A] {

  val stringDeserializer = new StringDeserializer

  override def configure(configs: java.util.Map[String, _], isKey: Boolean) =
    stringDeserializer.configure(configs, isKey)

  override def deserialize(topic: String, data: Array[Byte]) =
    Json.parse(stringDeserializer.deserialize(topic, data)).as[A]

  override def close() =
    stringDeserializer.close()

}

// TODO: Read Kafka config instead of specifying them explicitly
