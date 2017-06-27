package com.defender.logging

import org.scalatest._

class LoggingConfigSpec extends WordSpec
    with Matchers
    with LoggingConfig {
  "A LoggingConfig" must {
    "provide watchers entries specified in config file" in {
      val watchersEntries = configFromResourceFile("/logging.conf").watchersEntries
      watchersEntries.size shouldBe 2
    }
  }
}

