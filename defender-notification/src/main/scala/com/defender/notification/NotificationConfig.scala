package com.defender.notification

import java.io.File

import com.defender.api.ModuleConfig
import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.JavaConverters._
import scala.concurrent.duration._

trait NotificationConfig {
  case class NotifierEntry(
    id: String,
    notifierType: String,
    sources: Set[String],
    retryInterval: FiniteDuration,
    mail: MailSettings
  )

  def configFromFile(path: String): Config = {
    val f = new File(path)
    ConfigFactory.parseFile(f)
  }
  def configFromResourceFile(path: String): Config = {
    val f = new File(getClass.getResource(path).getFile)
    ConfigFactory.parseFile(f)
  }

  lazy val notificationConfig: Config = configFromResourceFile("/notification.conf")

  implicit class RichConfig(val underlying: Config) extends ModuleConfig {
    def toNotifierEntry(config: Config): NotifierEntry = {
      val id = config.getString("id")
      val notifierType = config.getString("type")
      val sources = config.getStringList("sources").asScala.toSet
      val retryInterval = config.getFiniteDuration("retry-interval")
      val mailConfig = config.getConfig("mail")
      val from = mailConfig.getString("from")
      val to = mailConfig.getString("to")
      val username = mailConfig.getString("username")
      val password = mailConfig.getString("password")
      val smtp: Map[String, AnyRef] = mailConfig.getConfig("smtp").entrySet().asScala
        .map(entry => (s"mail.smtp.${entry.getKey}", entry.getValue.unwrapped()))
        .toMap
      val mail = MailSettings(
        smtp,
        from,
        to,
        username,
        password
      )
      NotifierEntry(id, notifierType, sources, retryInterval, mail)
    }

    def notifiersEntries: Seq[NotifierEntry] =
      underlying.getConfigList("defender.notification.notifiers").asScala.map(toNotifierEntry)
  }
}

