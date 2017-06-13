package com.defender

import java.io.File

import com.typesafe.config._

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import java.util.Map.Entry

import scala.collection.mutable

object Configuration {
  private lazy val EnvConf: Config = {
    val path = System.getProperty("config.env.file")
    println(s"Environment configuration file: $path")
    try ConfigFactory.parseFile(new File(path)) catch {
      case e: Exception =>
        println(s"Can't load config: ${e.getMessage}")
        ConfigFactory.load()
    }
  }
  private lazy val Configuration: Config = {
    val path = System.getProperty("config.base.file")
    val config = try ConfigFactory.parseFile(new File(path)) catch {
      case e: Exception =>
        println(s"Can't load config: ${e.getMessage}")
        ConfigFactory.load()
    }
    println(s"Base configuration file: $path")
    EnvConf.withFallback(config)
  }
  lazy val AllConfig: Config = Configuration
  lazy val ApplicationConfig: Config = Configuration.getConfig("application")

  lazy val Port: Int = ApplicationConfig.getInt("port")

  object Watchers {
    private lazy val WatchersConf = ApplicationConfig.getConfig("watchers")
    lazy val Entries: Map[String, mutable.Set[(String, ConfigValue)]] = {
      def watcherName(entry: Entry[String, ConfigValue]) = {
        val k = entry.getKey
        k.substring(0, k.indexOf("."))
      }
      def mapPairs(pairs: (String, mutable.Set[Entry[String, ConfigValue]])) = pairs match {
        case (k, v) => (k, mapEntry(v))
      }
      def mapEntry(entries: mutable.Set[Entry[String, ConfigValue]]) = entries.map(entry => {
        val k = entry.getKey
        (k.substring(k.indexOf(".") + 1, k.length), entry.getValue)
      })

      ApplicationConfig.getConfig("server.watchers").entrySet().asScala
        .groupBy(watcherName)
        .map(mapPairs)
    }

    object AuthLog {
      private lazy val AuthLogConf = WatchersConf.getConfig("auth-log")

      lazy val Name = "auth-log"
      lazy val File: File = new File(AuthLogConf.getString("file"))
      lazy val PollInterval: FiniteDuration =
        Duration(AuthLogConf.getString("poll-interval")).asInstanceOf[FiniteDuration]
    }
  }

  object Notifier {
    private lazy val Notifier = ApplicationConfig.getConfig("notifier")

    lazy val RetryInterval: FiniteDuration =
      Duration(Notifier.getString("retry-interval")).asInstanceOf[FiniteDuration]
  }

  object Mail {
    lazy val Entries: Map[String, AnyRef] = ApplicationConfig.entrySet().asScala
      .filter(_.getKey.contains("mail.smtp"))
      .map(entry => (entry.getKey, entry.getValue.unwrapped()))
      .toMap
    private lazy val MailConf = ApplicationConfig.getConfig("mail")

    lazy val From: String = MailConf.getString("from")
    lazy val To: String = MailConf.getString("to")
    lazy val Username: String = MailConf.getString("username")
    lazy val Password: String = MailConf.getString("password")
  }
}
