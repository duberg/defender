package com.defender.logging

import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import java.time.temporal.ChronoField
import java.time.{ LocalDate, LocalDateTime, Duration => JDuration }

import com.defender.notification._

import scala.concurrent.duration.{ Duration, FiniteDuration }

object Implicits {
  implicit class LogEventLocalDateTime(value: String) {
    val formatter: DateTimeFormatter = new DateTimeFormatterBuilder()
      .appendPattern("MMM d HH:mm:ss")
      .parseDefaulting(ChronoField.YEAR, LocalDate.now.getYear)
      .toFormatter

    def toLocalDateTime: LocalDateTime = LocalDateTime.parse(value, formatter)
  }
  implicit class RichLocalDateTime(val localDateTime: LocalDateTime) extends AnyVal {
    def +(duration: Duration): LocalDateTime = {
      localDateTime.plus(JDuration.ofMillis(duration.toMillis))
    }
    def -(duration: Duration): LocalDateTime = {
      localDateTime.minus(JDuration.ofMillis(duration.toMillis))
    }
  }
  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)
  implicit object LogEventOrdering extends Ordering[LogEvent] {
    def compare(a: LogEvent, b: LogEvent): Int = a.localDateTime compareTo b.localDateTime
  }
  implicit def toNotifications(logEvents: LogEvents): Notifications = {
    logEvents.map(logEvent => {
      val fields = Set(
        "username" -> logEvent.username,
        "service" -> logEvent.service
      )
      Notification(logEvent.localDateTime, logEvent.message, fields)
    }).toSeq
  }
}

