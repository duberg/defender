package com.defender

import com.typesafe.config._

import scala.concurrent.duration._
import scala.collection.JavaConverters._

object Configuration {
  private lazy val Application = ConfigFactory.load()

  object Server {
    private lazy val Server = Application.getConfig("server")
    lazy val Port: Int = Server.getInt("port")
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
    lazy val RetryAfter: FiniteDuration =
      Duration(Mail.getString("retryAfter")).asInstanceOf[FiniteDuration]
  }
}
