package com.defender

import org.scalatest._
import scala.io.Source

class LogParserSpec extends FlatSpec {
  "A LogParser" should "parse log file" in {
    val lines = Source.fromResource("auth.log").getLines.toList
    assert(LogParser(lines).size == lines.size)
  }
}
