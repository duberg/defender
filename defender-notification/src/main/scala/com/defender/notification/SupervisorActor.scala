package com.defender.notification

import javax.mail.internet.AddressException

import akka.actor.SupervisorStrategy._
import akka.actor.{ Actor, ActorLogging, ActorRef, OneForOneStrategy, Props }
import com.defender.api.ActorLifecycleHooks
import com.defender.notification.SupervisorActor._
import com.sun.mail.util.MailConnectException

import scala.concurrent.duration._

class SupervisorActor extends Actor with ActorLogging with ActorLifecycleHooks {
  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: MailConnectException =>
        log.warning(e.getMessage)
        Resume
      case e: AddressException =>
        log.error(e, e.getMessage)
        Stop
      case e: Exception =>
        log.error(e, e.getMessage)
        Restart
    }
  def receive: PartialFunction[Any, Unit] = {
    case SuperviseRequest(p: Props, id: String) =>
      val ref = context.actorOf(p, id)
      sender() ! SuperviseResponse(ref)
      log.info(s"actor [${ref.path}] now supervised")
    case GetSupervisedRequest =>
      sender() ! GetSuperviseResponse(context.children.toList)
  }
}

object SupervisorActor {
  def props: Props = Props(new SupervisorActor)

  case class SuperviseRequest(p: Props, id: String)
  case class SuperviseResponse(supervised: ActorRef)
  case object GetSupervisedRequest
  case class GetSuperviseResponse(supervised: Seq[ActorRef])
}

