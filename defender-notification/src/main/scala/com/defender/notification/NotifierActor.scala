package com.defender.notification

import akka.actor.ActorRef
import akka.pattern.pipe
import com.defender.api.Persistence._
import com.defender.notification.NotifierActor._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

case class NotifierActorState(notifications: Notifications = Seq.empty) extends PersistentState[NotifierActorState] {
  def updated(persistentEvent: PersistentEvent): NotifierActorState = persistentEvent match {
    case AddedPersistentEvent(n) => copy(notifications ++ n)
    case RemovedPersistentEvent(n) => copy(notifications diff n)
  }
}

trait NotifierActor extends PersistentStateActor[NotifierActorState] {
  def sources: Set[ActorRef]
  def retryInterval: FiniteDuration
  def notify(n: Notifications): Unit = {
    val s = sender()
    val f = notifyRecipients(n)
      .map { notifications =>
        log.info("Recipients notified")
        RemoveCommand(notifications)
      }
      .recover {
        case e: Throwable =>
          context.system.scheduler.scheduleOnce(retryInterval, s, Notify)
          log.warning(s"Failed to notify recipients")
          e
      }
    f pipeTo s
  }
  def get(n: Notifications): Unit = sender() ! GetResponse(n)
  def add(n: Notifications, state: NotifierActorState): Unit = {
    val s = sender()
    if (n.nonEmpty) {
      persist(AddedPersistentEvent(n)) { persistentEvent =>
        context.become(active(state.updated(persistentEvent)))
        s ! Notify
        log.info(s"${n.size} notifications added")
      }
    } else s ! NotPersisted
  }
  def remove(n: Notifications, state: NotifierActorState): Unit = {
    val inter = n intersect state.notifications
    val s = sender()
    if (inter.nonEmpty) {
      persist(RemovedPersistentEvent(inter)) { persistentEvent =>
        context.become(active(state.updated(persistentEvent)))
        s ! Persisted
        log.info(s"${inter.size} notifications removed")
      }
    } else s ! NotPersisted
  }
  def subscribed(): Unit = log.info(s"New subscription: ${sender().path}")
  def subscriptionEvent(event: Subscription.Event, n: Notifications): Unit = {
    self ! AddCommand(n)
    sender() ! Subscription.Received(event)
  }
  def behavior(state: NotifierActorState): Receive = {
    case Notify if state.notifications.nonEmpty => notify(state.notifications)
    case GetRequest => get(state.notifications)
    case AddCommand(n) => add(n, state)
    case RemoveCommand(n) => remove(n, state)
    case Subscription.Subscribed => subscribed()
    case s @ Subscription.Event(eventId, n) => subscriptionEvent(s, n)
  }
  override def preStart(): Unit = {
    super.preStart()
    for (s <- sources) s ! Subscription.Subscribe
  }
  def notifyRecipients(notifications: Notifications): Future[Notifications]
}

object NotifierActor {
  private[notification] case object Notify

  sealed case class AddedPersistentEvent(notifications: Notifications) extends PersistentEvent
  sealed case class RemovedPersistentEvent(notifications: Notifications) extends PersistentEvent

  private[notification] case class AddCommand(notifications: Notifications) extends PersistentCommand
  private[notification] case class RemoveCommand(notifications: Notifications) extends PersistentCommand

  case object GetRequest
  case class GetResponse(notifications: Notifications)
}
