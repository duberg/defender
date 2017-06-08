package com.defender

import java.time.{ LocalDate, LocalDateTime }
import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import java.time.temporal.ChronoField

import scala.util.matching.Regex

object Parser {
  val DateTimePattern: Regex = """\w{3}  \d{1,2} \d\d:\d\d:\d\d""".r
  val UsernamePattern: Regex = "[a-z_][a-z0-9_-]*[$]?".r
  val ServicePattern: Regex = """[^ :]*""".r
  val MessagePattern: Regex = ".*".r
  val LogPattern: Regex = s"($DateTimePattern) ($UsernamePattern) ($ServicePattern): ($MessagePattern)".r

  val dtf: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("MMM  d HH:mm:ss")
    .parseDefaulting(ChronoField.YEAR, LocalDate.now.getYear)
    .toFormatter

  def parse(line: String): Option[Event] = line match {
    case LogPattern(d, u, s, m) => Option(Event(LocalDateTime.parse(d, dtf), u, s, m))
    case _ => None
  }
}