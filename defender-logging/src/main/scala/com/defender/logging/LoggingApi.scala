package com.defender.logging

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.slf4j.SLF4JLogging
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

class LoggingApi(
    val config: Config,
    val supervisor: ActorRef
)(implicit system: ActorSystem, executor: ExecutionContext, timeout: Timeout) extends LoggingConfig {
  val watchers: Seq[ActorRef] = for (c <- config.watchersEntries) yield {
    val f = new LogFilter(
      c.withinInterval,
      c.usernameMatch,
      c.serviceMatch,
      c.messageMatch
    )
    val r = new LogReader(c.file, f)
    val p = WatcherActor.props(c.id, r, c.watchInitialDelay, c.watchInterval, c.sweepInitialDelay, c.sweepInterval)
    val future = ask(supervisor, SupervisorActor.SuperviseRequest(p, c.id)).mapTo[SupervisorActor.SuperviseResponse].map(_.supervised)
    Await.result(future, 1 minute)
  }
  def logEvents: Future[Seq[LogEvent]] =
    Future.traverse(watchers)(ask(_, WatcherActor.GetRequest).mapTo[WatcherActor.GetResponse].map(_.logEvents.toSeq))
      .map(_.flatten)
}

object LoggingApi extends SLF4JLogging {
  def apply(id: String, config: Config)(implicit system: ActorSystem, executor: ExecutionContext, timeout: Timeout): LoggingApi = {
    val s = system.actorOf(SupervisorActor.props, id)
    val api = new LoggingApi(config, s)
    log.info(s"LoggingApi[$id] is up")
    api
  }
}

