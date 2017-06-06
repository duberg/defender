package com.defender.log

import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import java.time.temporal.ChronoField
import java.time.LocalDate

import scala.util.matching.Regex

object Log {
  val DateTimePattern: Regex = """\w{3}  \d{1,2} \d\d:\d\d:\d\d""".r
  val UsernamePattern: Regex = "[a-z_][a-z0-9_-]*[$]?".r
  val ServicePattern: Regex = """[^ :]*""".r
  val MessagePattern: Regex = ".*".r
  val LogPattern: Regex = s"($DateTimePattern) ($UsernamePattern) ($ServicePattern): ($MessagePattern)".r

  val AuthLogFilename = "/var/log/auth.log"

  val dtf: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("MMM  d HH:mm:ss")
    .parseDefaulting(ChronoField.YEAR, LocalDate.now.getYear)
    .toFormatter
}