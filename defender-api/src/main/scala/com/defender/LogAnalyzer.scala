package com.defender

import java.io.File

import com.defender.LogFilter._

import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source

class LogAnalyzer(val file: File, val filters: Seq[Filter])(implicit executor: ExecutionContext) {
  private var timestamp: Long = 0L

  def process: Future[Records] = Future {
    val t = file.lastModified()
    if (timestamp != t) {
      timestamp = t
      val bs = Source.fromFile(file)
      val lines = try bs.getLines().toSet finally bs.close()
      if (filters.isEmpty) LogParser(lines)
      else (LogParser(lines) /: filters)(_ filter _)
    } else Set.empty
  }
  def findOld(records: Records): Future[Records] = Future {
    records filter BeforeWeek
  }
}

object LogAnalyzer {
  def apply(file: File, filters: Seq[Filter] = Seq(AuthFailure, AfterWeek))(implicit executor: ExecutionContext): LogAnalyzer = new LogAnalyzer(file, filters)
}

