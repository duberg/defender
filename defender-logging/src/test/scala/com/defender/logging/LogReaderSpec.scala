package com.defender.logging

import org.scalatest._

class LogReaderSpec extends AsyncWordSpec
    with Matchers
    with LoggingConfig {
  "A LogReader" must {
    "read, parse, filter lines from log files specified in config file" in {
      val readers = configFromResourceFile("/logging.conf").readers
      val f1 = readers.head.read
      val f2 = readers(1).read
      for {
        r1 <- f1
        r2 <- f2
      } yield {
        r1.size shouldBe 67
        r2.size shouldBe 2
      }
    }
  }
}

