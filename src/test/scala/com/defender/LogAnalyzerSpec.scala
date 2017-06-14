package com.defender

import java.io.File
import java.time.LocalDateTime

import org.scalatest._
import Inspectors._

class LogAnalyzerSpec extends AsyncFlatSpec with Matchers {
  val f = new File(getClass.getResource("/auth.log").getFile)

  "A LogAnalyzer" should "analyze log file" in {
    val a = new LogAnalyzer(f, dateTimeFilter = _ => true)
    for (p <- a.process) yield {
      p.map(r => {
        r should not be empty
        forAll(r) { a.includeFilter(_) shouldBe true }
        forAll(r) { a.dateTimeFilter(_) shouldBe true }
      }).get
    }
  }
  "A LogAnalyzer" should "find old records" in {
    val record1 = LogRecord(LocalDateTime.now().minusDays(1), "", "", "")
    val record2 = LogRecord(LocalDateTime.now().minusDays(2), "", "", "")
    val record3 = LogRecord(LocalDateTime.now().minusWeeks(1), "", "", "")
    val record4 = LogRecord(LocalDateTime.now().minusWeeks(2), "", "", "")
    val records = Set(record1, record2, record3, record4)
    for (f <- LogAnalyzer.findOld(records)) yield {
      f.map(r => {
        r should contain only (record4, record3)
      }).get
    }
  }
}