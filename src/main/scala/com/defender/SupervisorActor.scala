package com.defender

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives.{ complete, get, onSuccess, pathEndOrSingleSlash }
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.defender.services.AuthLogService

import scala.concurrent._
import scala.concurrent.duration._
import scalatags.Text.all._

class SupervisorActor(
    implicit
    system: ActorSystem,
    materializer: ActorMaterializer,
    executor: ExecutionContext,
    timeout: Timeout
) extends Actor with ActorLogging {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._

  val notifierActor: ActorRef = context.actorOf(
    NotifierActor.props(
      SupervisorActor.NotifierActorId,
      Configuration.Notifier.RetryInterval
    ), SupervisorActor.NotifierActorId
  )
  val watcher = new Watcher(
    Configuration.Watchers.AuthLog.Name,
    Configuration.Watchers.AuthLog.File,
    Configuration.Watchers.AuthLog.PollInterval,
    notifierActor
  )

  val authLogService = new AuthLogService(watcher)

  def events: Future[String] = authLogService.events.map(events =>
    html(
      body(
        ol(
          for (x <- events) yield li(
            s"${x.localDateTime}, username: ${x.username}, sevice: ${x.service}, message: ${x.message}"
          )
        )
      )
    ).render)

  val route: Route = {
    pathEndOrSingleSlash {
      get {
        onSuccess(events) { ev =>
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, ev))
        }
      }
    }
  }

  val port = Configuration.Port
  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(route, "localhost", port)

  println(s"Defender online at http://localhost:$port/")

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: Exception =>
        log.error(e.getMessage, e)
        Restart
    }

  def receive: PartialFunction[Any, Unit] = {
    case p: Props => sender() ! context.actorOf(p)
  }
}

object SupervisorActor {
  def props(
    implicit
    system: ActorSystem,
    materializer: ActorMaterializer,
    executor: ExecutionContext,
    timeout: Timeout
  ): Props = Props(new SupervisorActor)

  val NotifierActorId = "notifier"
}