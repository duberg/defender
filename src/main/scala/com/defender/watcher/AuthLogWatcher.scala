package com.defender.watcher

import java.io.File
import java.time.LocalDateTime

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.defender.{ Configuration, Mail }
import com.defender.log.{ Log, _ }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

class AuthLogWatcher(implicit system: ActorSystem, timeout: Timeout) {
  import system.dispatcher
  import AuthLogWatcherActor._

  val watcher: ActorRef = system.actorOf(AuthLogWatcherActor.props, "watcher")

  system.scheduler.schedule(1 second, 1 minute, watcher, Analyze)

  def retrieveEvents: Future[Seq[Event]] = (watcher ? RetrieveEvents).mapTo[Seq[Event]]
}

private class AuthLogWatcherActor extends Actor with ActorLogging with ActorLifecycleHooks {
  import AuthLogWatcherActor._

  var fileSize = 0L
  var notificationPending = false
  var events: Seq[Event] = Seq()
  var notifications: Seq[Notification] = Seq()

  def receive: Receive = {
    case Analyze => analyze()
    case Notify => notification()
    case RetrieveEvents => sender() ! events
  }

  def analyze(): Unit = {
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
        log.info(s"new ${newEvents.size} events found")
        events ++= newEvents
        notifications ++= newEvents.map(Notification(_))
        self ! Notify
        log.info("analyze completed")
      }
    }
  }

  def notification(): Unit = {
    import context.dispatcher
    import scalatags.Text.all._
    val n = notifications.filterNot(_.notified)
    if (n.nonEmpty) {
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
        notificationPending = false
        log.info("notify completed")
      } else if (!notificationPending) {
        notificationPending = true
        context.system.scheduler.scheduleOnce(Configuration.mail.retryAfter, self, Notify)
        log.info(s"notify failed, retry after ${Configuration.mail.retryAfter}")
      }
    }
  }
}

private object AuthLogWatcherActor {
  case object Analyze
  case object Notify
  case object RetrieveEvents

  def props: Props = Props(new AuthLogWatcherActor)
}