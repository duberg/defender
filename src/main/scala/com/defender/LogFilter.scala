package com.defender

import java.time.LocalDateTime
import Implicits._

object LogFilter {
  val NotOlderThanThreeDays: LogRecord => Boolean = _.localDateTime > LocalDateTime.now().minusDays(3)
  val All: LogRecord => Boolean = _ => true
}
