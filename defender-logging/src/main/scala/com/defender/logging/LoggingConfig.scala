package com.defender.logging

import java.io.File

import com.defender.api.ModuleConfig
import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.matching.Regex

trait LoggingConfig {
  case class WatcherEntry(
    id: String,
    file: File,
    fileStr: String,
    withinInterval: FiniteDuration,
    usernameMatch: Regex,
    serviceMatch: Regex,
    messageMatch: Regex,
    watchInitialDelay: FiniteDuration,
    watchInterval: FiniteDuration,
    sweepInitialDelay: FiniteDuration,
    sweepInterval: FiniteDuration
  )

  def configFromFile(path: String): Config = {
    val f = new File(path)
    ConfigFactory.parseFile(f)
  }
  def configFromResourceFile(path: String): Config = {
    val f = new File(getClass.getResource(path).getFile)
    ConfigFactory.parseFile(f)
  }

  lazy val loggingConfig: Config = configFromResourceFile("/logging.conf")

  implicit class RichConfig(val underlying: Config) extends ModuleConfig {
    def toWatcherEntry(config: Config): WatcherEntry = {
      val id = config.getString("id")
      val file = config.getFile("file")
      val fileStr = config.getString("file")
      val withinInterval = config.getFiniteDuration("within-interval")
      val usernameMatch = config.getRegex("username-match")
      val serviceMatch = config.getRegex("service-match")
      val messageMatch = config.getRegex("message-match")
      val watchInitialDelay = config.getFiniteDuration("watch-initial-delay")
      val watchInterval = config.getFiniteDuration("watch-interval")
      val sweepInitialDelay = config.getFiniteDuration("sweep-initial-delay")
      val sweepInterval = config.getFiniteDuration("sweep-interval")

      WatcherEntry(
        id,
        file,
        fileStr,
        withinInterval,
        usernameMatch,
        serviceMatch,
        messageMatch,
        watchInitialDelay,
        watchInterval,
        sweepInitialDelay,
        sweepInterval
      )
    }

    def watchersEntries: Seq[WatcherEntry] =
      underlying.getConfigList("defender.logging.watchers").asScala.map(toWatcherEntry)

    def readers(implicit ex: ExecutionContext): Seq[LogReader] =
      for (c <- watchersEntries) yield {
        val filter = new LogFilter(
          c.withinInterval,
          c.usernameMatch,
          c.serviceMatch,
          c.messageMatch
        )
        val file = new File(getClass.getResource(c.fileStr).getFile)
        new LogReader(file, filter)
      }
  }
}

