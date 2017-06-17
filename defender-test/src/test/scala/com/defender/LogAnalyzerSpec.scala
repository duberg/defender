package com.defender

import com.defender.LogFilter._
import com.defender.utils.Base
import org.scalatest.Inspectors._
import org.scalatest.{ AsyncFlatSpec, _ }

class LogAnalyzerSpec extends AsyncFlatSpec with Matchers with Base {
  "A LogAnalyzer" should "process log file" in {
    for (r <- analyzer.process) yield {
      r should not be empty
      forAll(r) { AuthFailure(_) shouldBe true }
      forAll(r) { BeforeDay(_) shouldBe true }
    }
  }
  it should "find old records" in {
    val a = LogAnalyzer(file)
    for (r <- a.findOld(records)) yield {
      r should contain only (r3, r4)
    }
  }
}