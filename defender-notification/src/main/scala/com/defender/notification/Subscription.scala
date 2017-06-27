package com.defender.notification

import java.util.UUID

object Subscription {
  case object Subscribe
  case object Subscribed
  case class Received(event: Event)
  case class Event(id: UUID, notifications: Notifications)

  def newEvent(notifications: Notifications): Event = Event(UUID.randomUUID(), notifications)
}

