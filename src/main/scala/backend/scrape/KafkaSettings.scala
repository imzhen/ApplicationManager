package backend.scrape

/**
  * Created by Elliott on 3/28/17.
  */

trait KafkaSettings {
  val brokers = "localhost:9092"
  val topic = "test_topic"
  val group = "group1"
}
