package com.defender

import java.io.File

import akka.actor._
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.matching.Regex

class Watcher(
    name: String,
    file: File,
    matchPattern: Regex,
    pollInterval: FiniteDuration,
    notifier: ActorRef
)(implicit system: ActorSystem, timeout: Timeout) {
  import system.dispatcher

  val watcher: ActorRef = system.actorOf(WatcherActor.props(name, file, matchPattern, notifier), s"watcher-$name")

  system.scheduler.schedule(1 second, pollInterval, watcher, Watch)

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
  var events: Seq[Event] = Seq()

  def receiveCommand: Receive = {
    case Watch => watch()
    case GetEvents => sender() ! events
    case v: AddEvents =>
      persist(v) { v =>
        addEvents(v.events)
        notifier ! v
      }
  }

  val receiveRecover: Receive = {
    case AddEvents(ev) => addEvents(ev)
  }

  def addEvents(events: Seq[Event]): Unit = {
    this.events ++= events
    log.info(s"new ${events.size} events added")
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
        val newEvents = for {
          line <- bs.getLines().toList
          _ <- matchPattern findFirstIn line // todo
          event <- parse(line)
          if !(events contains event)
        } yield event
        if (newEvents.nonEmpty) self ! AddEvents(newEvents)
      } finally bs.close()
    }
  }
}

private case object Watch
private case class AddEvents(events: Seq[Event])
private case class RemoveEvents(events: Seq[Event])
case object GetEvents

private object WatcherActor {
  def props(name: String, file: File, matchPattern: Regex, notifier: ActorRef): Props =
    Props(new WatcherActor(name, file, matchPattern, notifier))
}