package com.defender.logging.parsing

import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import java.time.temporal.ChronoField
import java.time.{ LocalDate, LocalDateTime }

import com.defender.logging.LogEvent

import scala.util.parsing.combinator.RegexParsers

object Parser extends RegexParsers {
  def formatter: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("MMM d HH:mm:ss")
    .parseDefaulting(ChronoField.YEAR, LocalDate.now.getYear)
    .toFormatter
  def month: Parser[String] = """[a-zA-Z]{3}""".r ^^ { _.toString }
  def day: Parser[String] = """\d{1,2}""".r ^^ { _.toString }
  def time: Parser[String] = """\d{2}:\d{2}:\d{2}""".r ^^ { _.toString }
  def localDateTime: Parser[LocalDateTime] = {
    month ~ day ~ time ^^ {
      case month ~ day ~ time => LocalDateTime.parse(s"$month $day $time", formatter)
    }
  }
  def username: Parser[String] = "[a-z_][a-z0-9_-]*[$]?".r ^^ { _.toString }
  def service: Parser[String] = "[^: ]+".r ^^ { _.toString }
  def sep: Parser[String] = ": ".r ^^ { _.toString }
  def message: Parser[String] = ".*".r ^^ { _.toString }
  def expr: Parser[LogEvent] = {
    localDateTime ~ username ~ service ~ sep ~ message ^^ {
      case localDateTime ~ username ~ service ~ sep ~ message =>
        LogEvent(localDateTime, username, service, message)
    }
  }
  def parse(line: String): Option[LogEvent] = parse(expr, line) match {
    case Success(result, _) => Option(result)
    case failure: NoSuccess => scala.sys.error(
      s"""${getClass.getName} error on line:
         |[$line]
         |${failure.msg}
       """.stripMargin
    )
  }
}
