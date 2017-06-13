package com.defender

import java.io.File

import akka.actor._
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.defender.WatcherActor._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class Watcher(
    id: String,
    file: File,
    pollInterval: FiniteDuration,
    notifier: ActorRef
)(implicit context: ActorContext, timeout: Timeout) {
  import context.dispatcher

  val analyzer = new LogAnalyzer(file)
  val watcher: ActorRef = context.actorOf(WatcherActor.props(id, analyzer, notifier), id)

  context.system.scheduler.schedule(1 second, pollInterval, watcher, WatchRequest)(dispatcher, watcher)
  context.system.scheduler.schedule(1 second, 1 day, watcher, CleanRequest)(dispatcher, watcher)

  def event: Future[Seq[LogRecord]] = (watcher ? GetRequest)
    .mapTo[GetResponse]
    .map(_.records)
}

case class WatcherActorState(records: Seq[LogRecord] = Seq.empty) {
  def updated(event: WatcherEvent): WatcherActorState = event match {
    case AddedEvent(r) => copy(records ++ r)
    case RemovedEvent(r) => copy(records diff r)
  }
}

private class WatcherActor(
    id: String,
    analyzer: LogAnalyzer,
    notifier: ActorRef,
    private var state: WatcherActorState
) extends PersistentActor with ActorLogging with ActorLifecycleHooks {
  def persistenceId: String = id
  def receiveCommand: Receive = {
    case WatchRequest =>
      val r = analyzer.analyze.filterNot(state.records.contains(_))
      self ! AddCommand(r)
      context.system.scheduler
      if (self != sender()) sender() ! WatchResponse
    case CleanRequest =>
      val r = analyzer findOld state.records
      self ! RemoveCommand(r)
      if (self != sender()) sender() ! CleanResponse
    case GetRequest =>
      sender() ! GetResponse(state.records)
    case AddCommand(r) =>
      if (r.nonEmpty) {
        persist(AddedEvent(r)) { event =>
          state = state.updated(event)
          notifier ! NotifierActor.AddCommand(r)
          log.info(s"${r.size} log records added")
        }
      }
    case RemoveCommand(r) =>
      if (r.nonEmpty) {
        persist(RemovedEvent(r)) { event =>
          state = state.updated(event)
          log.info(s"${r.size} log records removed")
        }
      }
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
  ): Props =
    Props(new WatcherActor(id, analyzer, notifier, state))

  case class AddCommand(records: Seq[LogRecord])
  case class RemoveCommand(records: Seq[LogRecord])

  case object WatchRequest
  case object WatchResponse
  case object CleanRequest
  case object CleanResponse
  case object GetRequest
  case class GetResponse(records: Seq[LogRecord])

  sealed trait WatcherEvent
  case class AddedEvent(records: Seq[LogRecord]) extends WatcherEvent
  case class RemovedEvent(records: Seq[LogRecord]) extends WatcherEvent
}