package com.defender

import java.io.File
import java.time.LocalDateTime

import com.defender.Implicits._

import scala.io.Source

class LogAnalyzer(
    val file: File,
    val includeFilter: LogRecord => Boolean = _.message contains "authentication failure",
    val dateTimeFilter: LogRecord => Boolean = _.localDateTime > LocalDateTime.now().minusWeeks(1),
    private var timestamp: Long = 0L
) {
  def analyze: Seq[LogRecord] = {
    val t = file.lastModified()
    if (timestamp != t) {
      timestamp = t
      val bs = Source.fromFile(file)
      val lines = try bs.getLines().toList finally bs.close()
      LogParser(lines)
        .filter(dateTimeFilter)
        .filter(includeFilter)
    } else Seq.empty
  }
  def findOld(records: Seq[LogRecord]): Seq[LogRecord] = {
    val dateTimeFilter: LogRecord => Boolean = _.localDateTime > LocalDateTime.now().minusWeeks(1)
    records filterNot dateTimeFilter
  }
}
