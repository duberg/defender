package com.defender

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor

import scala.concurrent.duration.FiniteDuration

class NotifierActor(name: String, retryInterval: FiniteDuration)
    extends PersistentActor with ActorLogging with ActorLifecycleHooks {
  override def persistenceId = s"notifier-$name"

  var events: Seq[Event] = Seq()
  var pending = false

  def receiveCommand: Receive = {
    case Notify => notification()
    case v: AddEvents =>
      persist(v) { v =>
        addEvents(v.events)
        if (!pending) self ! Notify
      }
    case v: RemoveEvents =>
      persist(v) { v =>
        removeEvents(v.events)
      }
  }

  def receiveRecover: Receive = {
    case AddEvents(ev) => addEvents(ev)
  }

  def addEvents(events: Seq[Event]): Unit = {
    this.events ++= events
  }

  def removeEvents(events: Seq[Event]): Unit = {
    this.events = this.events diff events
  }

  def notification(): Unit = {
    import context.dispatcher
    import scalatags.Text.all._
    if (events.nonEmpty) {
      log.info("notify triggered")
      val notified = try {
        MailAgent.send(
          "Defender: authentication failure event",
          html(
            body(
              ol(
                for (x <- events) yield li(
                  s"${x.localDateTime}, username: ${x.username}, sevice: ${x.service}, message: ${x.message}"
                )
              )
            )
          ).render
        )
        true
      } catch {
        case e: MailAgentException =>
          log.error(e.message)
          false
      }
      if (notified) {
        pending = false
        self ! RemoveEvents(events)
        log.info("notify completed")
      } else {
        context.system.scheduler.scheduleOnce(retryInterval, self, Notify)
        log.info(s"notify failed, retry after $retryInterval")
      }
    }
  }
}

private object NotifierActor {
  def props(name: String, retryInterval: FiniteDuration): Props = Props(new NotifierActor(name, retryInterval))
}

private case object Notify
