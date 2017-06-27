package com.defender.notification

case class MailSettings(smtp: Map[String, AnyRef], from: String, to: String, username: String, password: String)
