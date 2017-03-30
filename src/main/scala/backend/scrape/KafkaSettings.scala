package backend.scrape

/**
  * Created by Elliott on 3/28/17.
  */

import com.typesafe.config.{Config, ConfigObject, ConfigValue}

trait KafkaSettings {
  val brokers = "localhost:9092"
  val topic = "test_topic"
  val group = "group1"
}

object ConfigSettings {

  def parseKafkaClientsProperties(config: Config): Map[String, String] = {
    def collectKeys(c: ConfigObject, prefix: String, keys: Set[String]): Set[String] = {
      var result = keys
      val iter = c.entrySet.iterator
      while (iter.hasNext()) {
        val entry = iter.next()
        entry.getValue match {
          case o: ConfigObject =>
            result ++= collectKeys(o, prefix + entry.getKey + ".", Set.empty)
          case s: ConfigValue =>
            result += prefix + entry.getKey
          case _ =>
        }
      }
      result
    }

    val keys = collectKeys(config.root, "", Set.empty)
    keys.map(key => key -> config.getString(key)).toMap
  }

}
