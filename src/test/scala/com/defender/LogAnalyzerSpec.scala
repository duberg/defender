package com.defender

import java.io.File
import java.time.LocalDateTime

import org.scalatest._
import Inspectors._

class LogAnalyzerSpec extends FlatSpec with Matchers {
  val f = new File(getClass.getResource("/auth.log").getFile)

  "A LogAnalyzer" should "analyze log file" in {
    val a = new LogAnalyzer(f, dateTimeFilter = _ => true)
    val records = a.analyze
    records should not be empty
    forAll(records) { a.includeFilter(_) shouldBe true }
    forAll(records) { a.dateTimeFilter(_) shouldBe true }
  }
  "A LogAnalyzer" should "find old records" in {
    val a = new LogAnalyzer(f)
    val record1 = LogRecord(LocalDateTime.now().minusDays(1), "", "", "")
    val record2 = LogRecord(LocalDateTime.now().minusDays(2), "", "", "")
    val record3 = LogRecord(LocalDateTime.now().minusWeeks(1), "", "", "")
    val record4 = LogRecord(LocalDateTime.now().minusWeeks(2), "", "", "")
    val records = Seq(record1, record2, record3, record4)
    val oldRecords = a findOld records
    oldRecords should contain only (record4, record3)
  }
}
