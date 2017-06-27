package com.defender.notification

object Implicits {
  implicit object NotificationOrdering extends Ordering[Notification] {
    def compare(a: Notification, b: Notification): Int = a.localDateTime compareTo b.localDateTime
  }
}

