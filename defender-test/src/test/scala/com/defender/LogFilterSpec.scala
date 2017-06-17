package com.defender

import com.defender.LogFilter._
import com.defender.utils.Base
import org.scalatest._

class LogFilterSpec extends FlatSpec with Matchers with Base {
  "A LogFilter" should "filter with AuthFailure" in {
    records filter AuthFailure should contain only (r1, r3)
  }
  it should "filter with BeforeDay" in {
    records filter BeforeDay should contain only (r1, r2, r3, r4)
  }
  it should "filter with AfterDay" in {
    records filter AfterDay should contain only (r5, r6, r7)
  }
  it should "filter with BeforeWeek" in {
    records filter BeforeWeek should contain only (r3, r4)
  }
  it should "filter with AfterWeek" in {
    records filter AfterWeek should contain only (r1, r2, r5, r6, r7)
  }
}