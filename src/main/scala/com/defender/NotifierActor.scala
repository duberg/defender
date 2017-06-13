package com.defender

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import com.defender.NotifierActor._
import com.defender.mail.MailSender

import scala.concurrent.duration.FiniteDuration

case class NotifierActorState(records: Seq[LogRecord] = Seq.empty) {
  def updated(event: NotifierEvent): NotifierActorState = event match {
    case AddedEvent(r) => copy(records ++ r)
    case RemovedEvent(r) => copy(records diff r)
  }
  def isEmpty: Boolean = records.isEmpty
  def nonEmpty: Boolean = records.nonEmpty
}

class NotifierActor(
  id: String,
  retryInterval: FiniteDuration,
  mailSender: MailSender,
  var state: NotifierActorState,
  var pending: Boolean
) extends PersistentActor
    with ActorLogging with ActorLifecycleHooks {
  def persistenceId: String = id
  def receiveCommand: Receive = {
    case NotifyRequest =>
      log.info("notify")
      val result = mailSender
        .withHandler { e =>
          log.error(e.getMessage, e)
          context.system.scheduler.scheduleOnce(retryInterval, self, NotifyRequest)(context.dispatcher, self)
        }
        .send(state.records)
      if (result) {
        self ! RemoveCommand(state.records)
        if (self != sender()) sender() ! NotifyResponse
      }
    case GetRequest =>
      sender() ! GetResponse(state.records)
    case AddCommand(r) =>
      if (r.nonEmpty) {
        persist(AddedEvent(r)) { event =>
          state = state.updated(event)
          self ! NotifyRequest
        }
      }
    case RemoveCommand(r) =>
      if (r.nonEmpty) {
        persist(RemovedEvent(r)) { event =>
          state = state.updated(event)
        }
      }
  }

  val receiveRecover: Receive = {
    case event: NotifierEvent => state = state.updated(event)
  }
}

private object NotifierActor {
  def props(
    id: String,
    retryInterval: FiniteDuration,
    mailSender: MailSender = new MailSender,
    state: NotifierActorState = NotifierActorState()
  ): Props = Props(new NotifierActor(id, retryInterval, mailSender, state, false))

  case class AddCommand(records: Seq[LogRecord])
  case class RemoveCommand(records: Seq[LogRecord])

  case object GetRequest
  case class GetResponse(records: Seq[LogRecord])
  case object NotifyRequest
  case object NotifyResponse

  sealed trait NotifierEvent
  case class AddedEvent(records: Seq[LogRecord]) extends NotifierEvent
  case class RemovedEvent(records: Seq[LogRecord]) extends NotifierEvent
}