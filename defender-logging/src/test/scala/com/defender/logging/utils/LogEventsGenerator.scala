package com.defender.logging.utils

import java.time.LocalDateTime

import com.defender.api.utils.IdGenerator
import com.defender.logging.{ LogEvent, LogEvents }

import scala.util.Random

trait LogEventsGenerator extends IdGenerator {
  def generateLogEvents(min: Int = 1, max: Int = 10): LogEvents = {
    val random = new Random()
    val n: Int = random.nextInt(max + 1 - min) + min
    (for (i <- 1 to n) yield {
      val id = generateId
      LogEvent(
        LocalDateTime.now().minusDays(i),
        s"user-$id",
        s"service-$id",
        ""
      )
    }).toSet
  }
}
