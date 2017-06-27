package com.defender.notification.utils

import java.time.LocalDateTime

import com.defender.api.utils.IdGenerator
import com.defender.notification._

import scala.util.Random

trait NotificationsGenerator extends IdGenerator {
  def generateNotifications(min: Int = 1, max: Int = 10): Notifications = {
    val random = new Random()
    val n: Int = random.nextInt(max + 1 - min) + min
    for (i <- 1 to n) yield {
      val id = generateId
      Notification(
        LocalDateTime.now().minusDays(i),
        s"message$n",
        (for (i <- 1 to n) yield (s"field$n", s"value$n")).toSet
      )
    }
  }
}
