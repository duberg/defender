package com.defender.logging

import akka.actor._
import akka.util.Timeout
import com.defender.api.Persistence._
import com.defender.logging.Implicits._
import com.defender.logging.WatcherActor._
import com.defender.notification._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success }

case class WatcherActorState(logEvents: LogEvents = Set.empty, lastModified: Long = 0L) extends PersistentState[WatcherActorState] {
  def updated(persistentEvent: PersistentEvent): WatcherActorState = persistentEvent match {
    case AddedPersistentEvent(l) => copy(logEvents ++ l)
    case AddedPersistentEventV1(l) => copy(logEvents ++ l)
    case RemovedPersistentEvent(l) => copy(logEvents -- l)
    case FileChangedPersistentEvent(l) => copy(logEvents, l)
  }
}

private class WatcherActor(
  val id: String,
  val reader: LogReader,
  val watchInitialDelay: FiniteDuration,
  val watchInterval: FiniteDuration,
  val sweepInitialDelay: FiniteDuration,
  val sweepInterval: FiniteDuration,
  val initSubscribers: Subscribers,
  val initState: WatcherActorState
)(implicit val executor: ExecutionContext, val timeout: Timeout)
    extends PersistentStateActor[WatcherActorState] with SubscriptionBehavior {
  def watch(lastModified: Long): Unit = {
    val l = reader.file.lastModified
    if (lastModified != l) sender() ! FileChangeCommand(l)
  }
  def sweep(l: LogEvents): Unit = {
    val rm = l.filter(_.localDateTime.isBefore(reader.filter.withinLocalDateTime))
    if (rm.nonEmpty) sender() ! RemoveCommand(rm)
  }
  def add(l: LogEvents, state: WatcherActorState): Unit = {
    val s = sender()
    val diff = l -- state.logEvents
    if (diff.nonEmpty) {
      persist(AddedPersistentEventV1(diff)) { persistentEvent =>
        changeState(state.updated(persistentEvent))
        notifySubscribers(Subscription.newEvent(diff))
        s ! Persisted
        log.info(s"${diff.size} log events added")
      }
    } else s ! NotPersisted
  }
  def remove(l: LogEvents, state: WatcherActorState): Unit = {
    val s = sender()
    val inter = l intersect state.logEvents
    if (inter.nonEmpty) {
      persist(RemovedPersistentEvent(inter)) { persistentEvent =>
        changeState(state.updated(persistentEvent))
        s ! Persisted
        log.info(s"${inter.size} log events removed")
      }
    } else s ! NotPersisted
  }
  def fileChange(l: Long, state: WatcherActorState): Unit = {
    val s = sender()
    persist(FileChangedPersistentEvent(l)) { persistentEvent =>
      changeState(state.updated(persistentEvent))
      reader.read onComplete {
        case Success(v) => if (v.nonEmpty) s ! AddCommand(v)
        case Failure(e) => s ! e
      }
    }
  }
  def get(l: LogEvents): Unit = sender() ! GetResponse(l)
  def baseBehavior(state: WatcherActorState): Receive = {
    case Watch => watch(state.lastModified)
    case Sweep => sweep(state.logEvents)
    case AddCommand(l) => add(l, state)
    case RemoveCommand(l) => remove(l, state)
    case FileChangeCommand(l) => fileChange(l, state)
    case GetRequest => get(state.logEvents)
  }
  def behavior(state: WatcherActorState): Receive =
    baseBehavior(state: WatcherActorState)
      .orElse(subscriptionBehavior)

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler.schedule(watchInitialDelay, watchInterval, self, Watch)
    context.system.scheduler.schedule(sweepInitialDelay, sweepInterval, self, Sweep)
  }
}

object WatcherActor {
  def props(
    id: String,
    reader: LogReader,
    watchInitialDelay: FiniteDuration,
    watchInitialInterval: FiniteDuration,
    sweepDelay: FiniteDuration,
    sweepInterval: FiniteDuration,
    subscribers: Subscribers = Set.empty,
    state: WatcherActorState = WatcherActorState()
  )(implicit executor: ExecutionContext, timeout: Timeout): Props = {
    Props(new WatcherActor(
      id,
      reader: LogReader,
      watchInitialDelay,
      watchInitialInterval,
      sweepDelay,
      sweepInterval,
      subscribers,
      state
    )(executor, timeout))
  }
  private[logging] case object Watch
  private[logging] case object Sweep

  sealed case class AddedPersistentEvent(s: LogEvents) extends PersistentEvent
  sealed case class AddedPersistentEventV1(logEvents2: LogEvents) extends PersistentEvent

  sealed case class RemovedPersistentEvent(logEvents: LogEvents) extends PersistentEvent
  sealed case class FileChangedPersistentEvent(lastModified: Long) extends PersistentEvent

  private[logging] case class AddCommand(logEvents: LogEvents) extends PersistentCommand
  private[logging] case class RemoveCommand(logEvents: LogEvents) extends PersistentCommand
  private[logging] case class FileChangeCommand(lastModified: Long) extends PersistentCommand

  case object GetRequest
  case class GetResponse(logEvents: LogEvents)
}

