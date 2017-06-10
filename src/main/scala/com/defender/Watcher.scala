package com.defender

import java.io.File
import java.time.LocalDateTime

import akka.actor._
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.matching.Regex
import Implicits._

class Watcher(
    name: String,
    file: File,
    matchPattern: Regex,
    pollInterval: FiniteDuration,
    notifier: ActorRef
)(implicit context: ActorContext, timeout: Timeout) {
  import context.dispatcher

  val watcher: ActorRef = context.actorOf(WatcherActor.props(name, file, matchPattern, notifier), s"watcher-$name")

  context.system.scheduler.schedule(1 second, pollInterval, watcher, Watch)
  context.system.scheduler.schedule(1 second, 1 day, watcher, Clean)

  def event: Future[Seq[Event]] = (watcher ? GetEvents).mapTo[Seq[Event]]
}

private class WatcherActor(
  name: String,
  file: File,
  matchPattern: Regex,
  notifier: ActorRef
) extends PersistentActor
    with ActorLogging with ActorLifecycleHooks {
  override def persistenceId = s"watcher-$name"

  var fileSize = 0L
  var cache: Seq[Event] = Seq()

  def receiveCommand: Receive = {
    case Watch => watch()
    case Clean => clean()
    case GetEvents => sender() ! cache
    case event: AddEvents =>
      persist(event) { ev =>
        addEvents(ev.events)
        notifier ! ev
      }
    case event: RemoveEvents =>
      persist(event) { ev =>
        removeEvents(ev.events)
      }
  }

  val receiveRecover: Receive = {
    case AddEvents(ev) => addEvents(ev)
  }

  def addEvents(events: Seq[Event]): Unit = {
    cache ++= events
  }

  def removeEvents(events: Seq[Event]): Unit = {
    cache = cache diff events
  }

  def clean(): Unit = {
    val ltd = LocalDateTime.now().minusDays(3)
    val events = cache.filter(_.ldt < ltd)
    if (events.nonEmpty) {
      self ! RemoveEvents(events)
      log.info(s"clean cache")
    }
  }

  def watch(): Unit = {
    import Parser.parse
    val s = file.length()
    if (s != fileSize) {
      log.info("log file has changed")
      fileSize = s
      val bs = Source.fromFile(file.getPath)
        .withClose(() => log.info("log file closed"))
      try {
        lazy val ltd = LocalDateTime.now().minusDays(3)
        val events = for {
          line <- bs.getLines().toList
          _ <- matchPattern findFirstIn line
          event <- parse(line)
          if event.ldt > ltd
          if !(cache contains event)
        } yield event
        if (events.nonEmpty) {
          log.info(s"new ${events.size} events found")
          self ! AddEvents(events)
        }
      } finally bs.close()
    }
  }
}

private case object Watch
private case object Clean
private case class AddEvents(events: Seq[Event])
private case class RemoveEvents(events: Seq[Event])
case object GetEvents

private object WatcherActor {
  def props(name: String, file: File, matchPattern: Regex, notifier: ActorRef): Props =
    Props(new WatcherActor(name, file, matchPattern, notifier))
}