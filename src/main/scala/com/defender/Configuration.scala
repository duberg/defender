package com.defender

import java.io.File

import com.typesafe.config._

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import java.util.Map.Entry

import scala.collection.mutable
import scala.util.matching.Regex

object Configuration {
  private lazy val Application = ConfigFactory.load()

  object Server {
    private lazy val Server = Application.getConfig("server")
    lazy val Port: Int = Server.getInt("port")

    object Watchers {
      private lazy val Watchers = Server.getConfig("watchers")
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

        Application.getConfig("server.watchers").entrySet().asScala
          .groupBy(watcherName)
          .map(mapPairs)
      }

      object AuthLog {
        private lazy val AuthLog = Watchers.getConfig("auth-log")

        lazy val Name = "auth-log"
        lazy val File: File = new File(AuthLog.getString("file"))
        lazy val MatchPattern: Regex = AuthLog.getString("match-pattern").r
        lazy val PollInterval: FiniteDuration =
          Duration(AuthLog.getString("poll-interval")).asInstanceOf[FiniteDuration]
      }

      object Notifier {
        private lazy val Notifier = Server.getConfig("notifier")

        lazy val RetryInterval: FiniteDuration =
          Duration(Notifier.getString("retry-interval")).asInstanceOf[FiniteDuration]
      }
    }

    object Notifier {
      private lazy val Notifier = Server.getConfig("notifier")

      lazy val RetryInterval: FiniteDuration =
        Duration(Notifier.getString("retry-interval")).asInstanceOf[FiniteDuration]
    }
  }

  object Mail {
    lazy val Entries: Map[String, AnyRef] = Application.entrySet().asScala
      .filter(_.getKey.contains("mail.smtp"))
      .map(entry => (entry.getKey, entry.getValue.unwrapped()))
      .toMap
    private lazy val Mail = Application.getConfig("mail")

    lazy val From: String = Mail.getString("from")
    lazy val To: String = Mail.getString("to")
    lazy val Username: String = Mail.getString("username")
    lazy val Password: String = Mail.getString("password")
  }
}
