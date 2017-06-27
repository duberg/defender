package com.defender.notification.utils

import akka.actor.{ ActorRef, Props }
import akka.testkit.{ DefaultTimeout, TestKit, TestProbe }
import akka.util.Timeout
import com.defender.notification.{ NotificationConfig, NotifierActor, NotifierActorState, _ }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }

trait NewNotifierActor extends NotificationsGenerator with NotificationConfig { _: TestKit with DefaultTimeout =>
  def newNotifierActor(
    state: Notifications = Seq.empty,
    stubNotifyRecipients: Notifications => Future[Notifications] = Future.successful
  ): (Set[TestProbe], ActorRef) = {
    val sources = Set(
      new TestProbe(system),
      new TestProbe(system)
    )
    val c = notificationConfig.notifiersEntries.head
    val id = s"${c.id}-$generateId"
    val t = timeout
    def props(_id: String, _sources: Set[ActorRef], _retryInterval: FiniteDuration, _state: NotifierActorState) = {
      Props(new NotifierActor {
        implicit def executor: ExecutionContext = system.dispatcher
        implicit def timeout: Timeout = t
        def id: String = _id
        def sources: Set[ActorRef] = _sources
        def retryInterval: FiniteDuration = _retryInterval
        def initState: NotifierActorState = _state
        def notifyRecipients(notifications: Notifications): Future[Notifications] = stubNotifyRecipients(notifications)
      })
    }
    val notifier = system.actorOf(props(id, sources.map(_.ref), c.retryInterval, NotifierActorState(state)), id)
    sources.foreach { source =>
      source.expectMsg(Subscription.Subscribe)
      source.reply(Subscription.Subscribed)
    }
    (sources, notifier)
  }

  def newNotifierActor(sources: Set[TestProbe]): ActorRef = {
    val c = notificationConfig.notifiersEntries.head
    val id = s"${c.id}-$generateId"
    val t = timeout
    def props(_id: String, _sources: Set[ActorRef], _retryInterval: FiniteDuration, _state: NotifierActorState) = {
      Props(new NotifierActor {
        implicit def executor: ExecutionContext = system.dispatcher
        implicit def timeout: Timeout = t
        def id: String = _id
        def sources: Set[ActorRef] = _sources
        def retryInterval: FiniteDuration = _retryInterval
        def initState: NotifierActorState = _state
        def notifyRecipients(notifications: Notifications): Future[Notifications] = Future.successful(notifications)
      })
    }
    val notifier = system.actorOf(props(id, sources.map(_.ref), c.retryInterval, NotifierActorState()), id)
    notifier
  }
}
