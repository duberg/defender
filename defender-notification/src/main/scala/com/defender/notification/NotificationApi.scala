package com.defender.notification

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.slf4j.SLF4JLogging
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

class NotificationApi(
    val config: Config,
    val supervisor: ActorRef
)(implicit system: ActorSystem, executor: ExecutionContext, timeout: Timeout) extends NotificationConfig with SLF4JLogging {
  val notifiers: Seq[ActorRef] = for (c <- config.notifiersEntries) yield {
    c.notifierType match {
      case "mail" =>
        val sources: Set[ActorRef] =
          Await.result(Future.traverse(c.sources)(source => system.actorSelection(s"*/*/$source").resolveOne()), 1 minute)
        val p = MailNotifierActor.props(c.id, sources, c.retryInterval, c.mail)
        val future = ask(supervisor, SupervisorActor.SuperviseRequest(p, c.id)).mapTo[SupervisorActor.SuperviseResponse].map(_.supervised)
        Await.result(future, 1 minute)
    }
  }
}

object NotificationApi extends SLF4JLogging {
  def apply(id: String, config: Config)(implicit system: ActorSystem, executor: ExecutionContext, timeout: Timeout): NotificationApi = {
    val s = system.actorOf(SupervisorActor.props, id)
    val api = new NotificationApi(config, s)
    log.info(s"NotificationApi[$id] is up")
    api
  }
}

