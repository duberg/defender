package com.defender.notification

import akka.actor.Actor

trait SubscriptionBehavior { _: Actor =>
  private var subscribers: Subscribers = initSubscribers

  val subscriptionBehavior: Receive = {
    case Subscription.Subscribe =>
      val s = sender()
      subscribers += s
      s ! Subscription.Subscribed
    case _: Subscription.Received =>
  }

  def initSubscribers: Subscribers
  def notifySubscribers(event: Subscription.Event): Unit = for (s <- subscribers) s ! event
}

