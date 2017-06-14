package com.defender

import java.io.File
import java.time.LocalDateTime

import akka.util.Timeout
import com.defender.Implicits._

import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source

class LogAnalyzer(
    val file: File,
    val includeFilter: LogRecord => Boolean = _.message contains "authentication failure",
    val dateTimeFilter: LogRecord => Boolean = _.localDateTime > LocalDateTime.now().minusWeeks(1),
    private var timestamp: Long = 0L
)(implicit executor: ExecutionContext) {
  def process: Future[Option[Records]] = Future {
    val t = file.lastModified()
    if (timestamp != t) {
      timestamp = t
      val bs = Source.fromFile(file)
      val lines = try bs.getLines().toList finally bs.close()
      val r = LogParser(lines)
        .filter(dateTimeFilter)
        .filter(includeFilter)
        .toSet
      if (r.isEmpty) None else Option(r)
    } else None
  }
}

object LogAnalyzer {
  def findOld(records: Records)(implicit executor: ExecutionContext): Future[Option[Records]] = Future {
    val dateTimeFilter: LogRecord => Boolean = _.localDateTime > LocalDateTime.now().minusWeeks(1)
    val r = records filterNot dateTimeFilter
    if (r.isEmpty) None else Option(r)
  }
}
