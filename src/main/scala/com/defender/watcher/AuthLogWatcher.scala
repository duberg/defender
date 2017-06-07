package com.defender.watcher

import java.io.File
import java.time.LocalDateTime

import akka.actor._
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.defender.{ Configuration, Mail }
import com.defender.log.{ Log, _ }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

class AuthLogWatcher(implicit system: ActorSystem, timeout: Timeout) {
  import system.dispatcher

  val watcher: ActorRef = system.actorOf(AuthLogWatcherActor.props, "watcher")

  system.scheduler.schedule(1 second, 1 minute, watcher, Watch)

  def retrieveEvents: Future[Seq[Event]] = (watcher ? RetrieveEvents).mapTo[Seq[Event]]
}

private class AuthLogWatcherActor extends PersistentActor with ActorLogging with ActorLifecycleHooks {
  override def persistenceId = "auth-log-watcher"

  var fileSize = 0L
  var notificationPending = false
  var events: Seq[Event] = Seq()
  var notifications: Seq[Notification] = Seq()

  def receiveCommand: Receive = {
    case Watch => watch()
    case Notify => notification()
    case RetrieveEvents => sender() ! events
    case v: AddEvents =>
      persist(v) { v =>
        addEvents(v.events)
        self ! Notify
      }
    case Notified =>
      persist(Notified) { _ =>
        updateNotifications()
      }
  }

  val receiveRecover: Receive = {
    case AddEvents(e) => addEvents(e)
    case Notified => updateNotifications()
  }

  def addEvents(events: Seq[Event]): Unit = {
    this.events ++= events
    this.notifications ++= events.map(Notification(_))
  }

  def updateNotifications(): Unit = {
    this.notifications = notifications.map { n =>
      if (!n.notified) n.copy(notified = true) else n
    }
  }

  def watch(): Unit = {
    import Log._
    def parse(line: String): Option[Event] = line match {
      case LogPattern(d, u, s, m) =>
        Option(Event(LocalDateTime.parse(d, dtf), u, s, m))
      case _ =>
        log.warning(s"can't parse log line: $line")
        None
    }
    val size = new File(AuthLogFilename).length()
    if (size != fileSize) {
      log.info("log file has changed")
      fileSize = size
      val bs = Source.fromFile(AuthLogFilename)
        .withClose(() => log.info("log file closed"))
      val newEvents = try {
        for {
          l <- bs.getLines().toList
          if l contains "authentication failure"
          e <- parse(l)
          if !(events contains e)
        } yield e
      } finally bs.close()
      if (newEvents.nonEmpty) {
        self ! AddEvents(newEvents)
        log.info(s"new ${newEvents.size} events found")
      }
    }
  }

  def notification(): Unit = {
    import context.dispatcher
    import scalatags.Text.all._
    val events = notifications
      .filterNot(_.notified)
      .map(_.event)
    if (events.nonEmpty) {
      log.info("notify triggered")
      val notified = Mail.send(
        "Defender: authentication failure event",
        html(
          body(
            ol(
              for (e <- events) yield li(
                s"${e.localDateTime}, username: ${e.username}, sevice: ${e.service}, message: ${e.message}"
              )
            )
          )
        ).render
      )
      if (notified) {
        self ! Notified
        log.info("notify completed")
      } else if (!notificationPending) {
        notificationPending = true
        context.system.scheduler.scheduleOnce(Configuration.Mail.RetryAfter, self, Notify)
        log.info(s"notify failed, retry after ${Configuration.Mail.RetryAfter}")
      }
    }
  }
}

case object Watch
case object Notify
case object Notified
case object RetrieveEvents
case class AddEvents(events: Seq[Event])

private object AuthLogWatcherActor {
  def props: Props = Props(new AuthLogWatcherActor)
}