package com.defender

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.defender.NotifierActor._
import com.defender.mail.MailSender

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

case class NotifierActorState(records: Records = Set.empty) {
  def updated(event: NotifierEvent): NotifierActorState = event match {
    case AddedEvent(r) => copy(records ++ r)
    case RemovedEvent(r) => copy(records -- r)
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
)(implicit executor: ExecutionContext, timeout: Timeout) extends PersistentActor
    with ActorLogging with ActorLifecycleHooks {
  def persistenceId: String = id
  def receiveCommand: Receive = {
    case NotifyRequest =>
      val s = sender()
      def ms = mailSender
        .withHandler { e =>
          log.error(e.getMessage, e)
          context.system.scheduler.scheduleOnce(retryInterval, self, NotifyRequest)(context.dispatcher, self)
        }
      for {
        r <- ms.send(state.records)
        _ <- r
      } {
        self ! RemoveCommand(state.records)
        if (self != s) s ! NotifyResponse
        log.info("notify completed")
      }
    case GetRequest =>
      sender() ! GetResponse(state.records)
    case AddCommand(r) =>
      // must not persist event with same records
      val diff = r -- state.records
      if (diff.nonEmpty) {
        persist(AddedEvent(diff)) { event =>
          state = state.updated(event)
          self ! NotifyRequest
          if (self != sender()) sender() ! Persisted
        }
      } else if (self != sender()) sender() ! NotPersisted
    case RemoveCommand(r) =>
      // must not persist event with already removed records
      val inter = r intersect state.records
      if (inter.nonEmpty) {
        persist(RemovedEvent(inter)) { event =>
          state = state.updated(event)
          if (self != sender()) sender() ! Persisted
        }
      } else if (self != sender()) sender() ! NotPersisted
  }

  val receiveRecover: Receive = {
    case event: NotifierEvent => state = state.updated(event)
  }
}

private object NotifierActor {
  def props(
    id: String,
    retryInterval: FiniteDuration,
    mailSender: MailSender,
    state: NotifierActorState = NotifierActorState()
  )(implicit executor: ExecutionContext, timeout: Timeout): Props = Props(new NotifierActor(id, retryInterval, mailSender, state, false))

  case class AddCommand(records: Records)
  case class RemoveCommand(records: Records)
  case object Persisted
  case object NotPersisted

  case object GetRequest
  case class GetResponse(records: Records)
  case object NotifyRequest
  case object NotifyResponse

  sealed trait NotifierEvent
  sealed case class AddedEvent(records: Records) extends NotifierEvent
  sealed case class RemovedEvent(records: Records) extends NotifierEvent
}
