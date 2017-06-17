package com.defender.utils

import java.io.File
import java.time.LocalDateTime

import com.defender.{ LogAnalyzer, LogFilter, LogRecord }

import scala.concurrent.ExecutionContext

trait Base {
  val r1 = LogRecord(LocalDateTime.now().minusDays(1), "r1", "", "record1: authentication failure")
  val r2 = LogRecord(LocalDateTime.now().minusDays(2), "r2", "", "")
  val r3 = LogRecord(LocalDateTime.now().minusWeeks(1), "r3", "", "record3: authentication failure")
  val r4 = LogRecord(LocalDateTime.now().minusWeeks(2), "r4", "", "")
  val r5 = LogRecord(LocalDateTime.now(), "r5", "", "")
  val r6 = LogRecord(LocalDateTime.now().minusHours(2), "r6", "", "")
  val r7 = LogRecord(LocalDateTime.now().plusHours(2), "r7", "", "failure")
  val records = Set(r1, r2, r3, r4, r5, r6, r7)

  val file = new File(getClass.getResource("/auth.log").getFile)

  val filters = Seq(
    LogFilter.AuthFailure,
    LogFilter.BeforeDay
  )

  def analyzer(implicit executor: ExecutionContext): LogAnalyzer = LogAnalyzer(file, filters)
}
