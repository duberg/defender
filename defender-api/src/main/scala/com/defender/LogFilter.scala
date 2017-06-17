package com.defender

import java.time.LocalDateTime

object LogFilter {
  type Filter = LogRecord => Boolean

  val AuthFailure: Filter = _.message.contains("authentication failure")
  val AfterWeek: Filter = _.localDateTime.isAfter(LocalDateTime.now().minusWeeks(1))
  val AfterDay: Filter = _.localDateTime.isAfter(LocalDateTime.now().minusDays(1))
  val BeforeWeek: Filter = _.localDateTime.isBefore(LocalDateTime.now().minusWeeks(1))
  val BeforeDay: Filter = _.localDateTime.isBefore(LocalDateTime.now().minusDays(1))
}
