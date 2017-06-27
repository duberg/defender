package com.defender.logging.utils

import java.io.File

import akka.actor.ActorRef
import akka.testkit.{ DefaultTimeout, TestKit, TestProbe }
import com.defender.logging._

import scala.concurrent.duration._

trait NewWatcherActor extends LogEventsGenerator with LoggingConfig { _: TestKit with DefaultTimeout =>
  import system.dispatcher // !!! else uses incorrect ExecutionContext

  def newWatcherActor(): (TestProbe, ActorRef) = {
    val probe = new TestProbe(system)
    val c = loggingConfig.watchersEntries.head
    val id = s"${c.id}-$generateId"
    val reader = loggingConfig.readers.head
    val watcher = system.actorOf(WatcherActor.props(
      id,
      reader,
      0 second,
      1 day,
      1 day,
      1 day,
      Set(probe.ref)
    ), id)
    (probe, watcher)
  }
  def newWatcherActorWithSubscribers(): (Seq[TestProbe], ActorRef) = {
    val probes = for (p <- 1 to 10) yield new TestProbe(system)
    val c = loggingConfig.watchersEntries.head
    val id = s"${c.id}-$generateId"
    val reader = loggingConfig.readers.head
    val watcher = system.actorOf(WatcherActor.props(
      id,
      reader,
      0 second,
      1 day,
      1 day,
      1 day,
      probes.map(_.ref).toSet
    ), id)
    (probes, watcher)
  }
  def newWatcherActorWithState(state: LogEvents): (TestProbe, ActorRef) = {
    val probe = new TestProbe(system)
    val c = loggingConfig.watchersEntries.head
    val id = s"${c.id}-$generateId"
    val reader = loggingConfig.readers.head
    val watcher = system.actorOf(WatcherActor.props(
      id,
      reader,
      1 day,
      1 day,
      1 day,
      1 day,
      Set(probe.ref),
      WatcherActorState(state)
    ), id)
    (probe, watcher)
  }
  def newWatcherActorWithFileNotFoundException(): (TestProbe, ActorRef) = {
    val file = new File("/test")
    val filter = new LogFilter(
      1 day,
      ".*".r,
      ".*".r,
      ".*".r
    )
    val reader = new LogReader(file, filter)
    val probe = new TestProbe(system)
    val c = loggingConfig.watchersEntries.head
    val id = s"${c.id}-$generateId"
    val watcher = system.actorOf(WatcherActor.props(
      id,
      reader,
      1 day,
      1 day,
      1 day,
      1 day,
      Set(probe.ref)
    ), id)
    (probe, watcher)
  }
}

