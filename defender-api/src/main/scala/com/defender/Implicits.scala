package com.defender

import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import java.time.temporal.ChronoField
import java.time.{ LocalDate, LocalDateTime }

object Implicits {
  implicit class LogRecordLocalDateTime(value: String) {
    val formatter: DateTimeFormatter = new DateTimeFormatterBuilder()
      .appendPattern("MMM d HH:mm:ss")
      .parseDefaulting(ChronoField.YEAR, LocalDate.now.getYear)
      .toFormatter

    def toLocalDateTime: LocalDateTime = LocalDateTime.parse(value, formatter)
  }
  implicit object LogRecordOrdering extends Ordering[LogRecord] {
    def compare(a: LogRecord, b: LogRecord): Int = a.localDateTime compareTo b.localDateTime
  }
}
