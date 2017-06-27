package com.defender.notification.utils

import akka.actor.{ Actor, Props }
import com.defender.notification.Subscription

class SubscriptionActor extends Actor {
  def receive: Receive = {
    case message @ Subscription.Subscribe =>
      sender() ! Subscription.Subscribed
      context.parent.forward(message)
  }
}

object SubscriptionActor {
  def props = Props(new SubscriptionActor)
}
