package com.defender

import akka.actor._
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.defender.WatcherActor._

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

class Watcher(
    id: String,
    pollInterval: FiniteDuration,
    watcherRef: ActorRef,
    notifierRef: ActorRef,
    analyzer: LogAnalyzer
)(implicit system: ActorSystem, executor: ExecutionContext, timeout: Timeout) {
  system.scheduler.schedule(1 second, pollInterval, watcherRef, WatchRequest)(executor, watcherRef)
  system.scheduler.schedule(1 second, 1 day, watcherRef, CleanRequest)(executor, watcherRef)

  def records: Future[Records] = (watcherRef ? GetRequest)
    .mapTo[GetResponse]
    .map(_.records)
}

case class WatcherActorState(records: Records = Set.empty) {
  def updated(event: WatcherEvent): WatcherActorState = event match {
    case AddedEvent(r) => copy(records ++ r)
    case RemovedEvent(r) => copy(records -- r)
  }
}

private class WatcherActor(
    id: String,
    analyzer: LogAnalyzer,
    notifier: ActorRef,
    private var state: WatcherActorState
)(implicit executor: ExecutionContext, timeout: Timeout) extends PersistentActor with ActorLogging with ActorLifecycleHooks {
  def persistenceId: String = id
  def receiveCommand: Receive = {
    case WatchRequest =>
      val s = sender() // only inside actor
      for (p <- analyzer.process) {
        p.foreach(self ! AddCommand(_))
        if (self != s) s ! WatchResponse
      }
    case CleanRequest =>
      val s = sender() // only inside actor
      for (r <- LogAnalyzer findOld state.records) {
        r.foreach(self ! RemoveCommand(_))
        if (self != s) s ! CleanResponse
        log.info("clean request completed")
      }
    case GetRequest =>
      sender() ! GetResponse(state.records)
    case AddCommand(r) =>
      // must not persist event with same records
      val diff = r -- state.records
      if (diff.nonEmpty) {
        persist(AddedEvent(diff)) { event =>
          state = state.updated(event)
          notifier ! NotifierActor.AddCommand(diff)
          if (self != sender()) sender() ! Persisted
          log.info(s"${diff.size} log records added")
        }
      } else if (self != sender()) sender() ! NotPersisted
    case RemoveCommand(r) =>
      // must not persist event with already removed records
      val inter = r intersect state.records
      if (inter.nonEmpty) {
        persist(RemovedEvent(inter)) { event =>
          state = state.updated(event)
          if (self != sender()) sender() ! Persisted
          log.info(s"${inter.size} log records removed")
        }
      } else if (self != sender()) sender() ! NotPersisted
  }

  val receiveRecover: Receive = {
    case event: WatcherEvent => state = state.updated(event)
  }
}

private object WatcherActor {
  def props(
    id: String,
    analyzer: LogAnalyzer,
    notifier: ActorRef,
    state: WatcherActorState = WatcherActorState()
  )(implicit executor: ExecutionContext, timeout: Timeout): Props =
    Props(new WatcherActor(id, analyzer, notifier, state))

  case class AddCommand(records: Records)
  case class RemoveCommand(records: Records)
  case object Persisted
  case object NotPersisted

  case object WatchRequest
  case object WatchResponse
  case object CleanRequest
  case object CleanResponse
  case object GetRequest
  case class GetResponse(records: Records)

  sealed trait WatcherEvent
  sealed case class AddedEvent(records: Records) extends WatcherEvent
  sealed case class RemovedEvent(records: Records) extends WatcherEvent
}