package com.defender

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor

import scala.concurrent.duration.FiniteDuration

class NotifierActor(name: String, retryInterval: FiniteDuration)
    extends PersistentActor with ActorLogging with ActorLifecycleHooks {
  override def persistenceId = s"notifier-$name"

  var cache: Seq[Event] = Seq()
  var pending = false

  def receiveCommand: Receive = {
    case Notify => notification()
    case event: AddEvents =>
      persist(event) { ev =>
        addEvents(ev.events)
        if (!pending) self ! Notify
      }
    case event: RemoveEvents =>
      persist(event) { ev =>
        removeEvents(ev.events)
      }
  }

  def receiveRecover: Receive = {
    case AddEvents(ev) => addEvents(ev)
    case RemoveEvents(ev) => removeEvents(ev)
  }

  def addEvents(events: Seq[Event]): Unit = {
    cache ++= events
  }

  def removeEvents(events: Seq[Event]): Unit = {
    cache = cache diff events
  }

  def notification(): Unit = {
    import context.dispatcher
    import scalatags.Text.all._
    if (cache.nonEmpty) {
      log.info("notify triggered")
      val notified = try {
        MailAgent.send(
          "Defender: authentication failure event",
          html(
            body(
              ol(
                for (x <- cache) yield li(
                  s"${x.ldt}, username: ${x.username}, sevice: ${x.service}, message: ${x.message}"
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
        self ! RemoveEvents(cache)
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
