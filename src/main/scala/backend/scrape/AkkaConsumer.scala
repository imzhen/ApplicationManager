package backend.scrape

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase, Observer}


object AkkaConsumer extends App with MongoDBConf with KafkaSettings {

  implicit val system = ActorSystem.create("akka-stream-kafka")
  implicit val materializer = ActorMaterializer()

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new JsonDeserializer[Record])
    .withBootstrapServers(brokers)
    .withGroupId(group)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

  val consumerGraphSimple = Consumer.committableSource(consumerSettings, Subscriptions.topics(topic))
    .mapAsync(1) { msg =>
      collection.insertOne(msg.record.value()).subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = ()

        override def onError(e: Throwable): Unit = println("Failed")

        override def onComplete(): Unit = ()
      })
      msg.committableOffset.commitScaladsl()
    }
    .to(Sink.ignore)

  consumerGraphSimple.run()

}

trait MongoDBConf {
  val mongoClient: MongoClient = MongoClient()
  val database: MongoDatabase = mongoClient.getDatabase("BppManager")
  val collection: MongoCollection[Document] = database.getCollection("Kpplication")
}
