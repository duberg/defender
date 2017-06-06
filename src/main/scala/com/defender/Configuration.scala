package com.defender

import com.typesafe.config._

import scala.concurrent.duration._
import scala.collection.JavaConverters._

object Configuration {
  private lazy val configuration = ConfigFactory.load()

  object server {
    private lazy val server = configuration.getConfig("server")
    lazy val port: Int = server.getInt("port")
  }

  object mail {
    lazy val toMap: Map[String, AnyRef] = configuration.entrySet().asScala
      .filter(_.getKey.contains("mail.smtp"))
      .map(entry => (entry.getKey, entry.getValue.unwrapped()))
      .toMap
    private lazy val mail = configuration.getConfig("mail")

    lazy val from: String = mail.getString("from")
    lazy val to: String = mail.getString("to")
    lazy val username: String = mail.getString("username")
    lazy val password: String = mail.getString("password")
    lazy val retryAfter: FiniteDuration =
      Duration(mail.getString("retryAfter")).asInstanceOf[FiniteDuration]
  }
}
