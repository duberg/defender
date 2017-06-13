package com.defender

import java.time.{ LocalDate, LocalDateTime }
import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import java.time.temporal.ChronoField

object Implicits {
  implicit class RichLocalDateTime(value: LocalDateTime) {
    def >(localDateTime: LocalDateTime): Boolean = this.value.compareTo(localDateTime) > 1
    def <(localDateTime: LocalDateTime): Boolean = this.value.compareTo(localDateTime) <= -1
    def ==(localDateTime: LocalDateTime): Boolean = this.value.compareTo(localDateTime) == 0
  }
  implicit class LogRecordLocalDateTime(value: String) {
    val formatter: DateTimeFormatter = new DateTimeFormatterBuilder()
      .appendPattern("MMM d HH:mm:ss")
      .parseDefaulting(ChronoField.YEAR, LocalDate.now.getYear)
      .toFormatter

    def toLocalDateTime: LocalDateTime = LocalDateTime.parse(value, formatter)
  }
}

