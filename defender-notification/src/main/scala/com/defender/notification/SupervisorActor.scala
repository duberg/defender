package com.defender.notification

import java.io.FileNotFoundException

import akka.actor.SupervisorStrategy._
import akka.actor.{ Actor, ActorLogging, ActorRef, OneForOneStrategy, Props }
import com.defender.api.ActorLifecycleHooks
import com.defender.notification.SupervisorActor._

import scala.concurrent.duration._

class SupervisorActor extends Actor with ActorLogging with ActorLifecycleHooks {
  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: FileNotFoundException =>
        log.error(e, e.getMessage)
        Resume
      case e: Exception =>
        log.error(e, e.getMessage)
        Resume
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

