package com.defender.logging

import java.time.LocalDateTime

import com.defender.logging.Implicits._

import scala.concurrent.duration._
import scala.util.matching.Regex

class LogFilter(
    val withinInterval: Duration,
    val usernameMatch: Regex,
    val serviceMatch: Regex,
    val messageMatch: Regex
) {
  val withinLocalDateTime: LocalDateTime = LocalDateTime.now() - withinInterval
  def isAfter(logEvent: LogEvent): Boolean = {
    withinInterval match {
      case Duration.Zero => true
      case _ => logEvent.localDateTime.isAfter(withinLocalDateTime)
    }
  }
  def isUsernameMatch(logEvent: LogEvent): Boolean =
    usernameMatch.findFirstIn(logEvent.username).exists(_ => true)
  def isServiceMatch(logEvent: LogEvent): Boolean =
    serviceMatch.findFirstIn(logEvent.service).exists(_ => true)
  def isMessageMatch(logEvent: LogEvent): Boolean =
    messageMatch.findFirstIn(logEvent.message).exists(_ => true)
  def filter(logEvents: LogEvents): LogEvents =
    logEvents
      .filter(isAfter)
      .filter(isUsernameMatch)
      .filter(isServiceMatch)
      .filter(isMessageMatch)
  def matchAll(logEvent: LogEvent): Boolean =
    isAfter(logEvent) && isUsernameMatch(logEvent) && isServiceMatch(logEvent) && isMessageMatch(logEvent)
}
