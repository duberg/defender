package com.defender

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.defender.services.AuthLogService

import scala.concurrent.Future
import scala.language.postfixOps
import scalatags.Text.all._

object Boot extends App with JsonSupport {
  implicit val system = ActorSystem("root", Configuration.AllConfig)
  implicit val materializer = ActorMaterializer()
  val defender = system.actorOf(Supervisor.props(system, materializer), "defender")
}

class Supervisor(implicit system: ActorSystem, materializer: ActorMaterializer) extends Actor with ActorLogging {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import system.dispatcher

  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 seconds)

  val notifierActor: ActorRef = context.actorOf(NotifierActor.props("default", Configuration.Notifier.RetryInterval), "notifier-default")
  val watcher = new Watcher(
    Configuration.Watchers.AuthLog.Name,
    Configuration.Watchers.AuthLog.File,
    Configuration.Watchers.AuthLog.MatchPattern,
    Configuration.Watchers.AuthLog.PollInterval,
    notifierActor
  )

  val authLogService = new AuthLogService(watcher)

  def events: Future[String] = authLogService.events.map(events =>
    html(
      body(
        ol(
          for (x <- events) yield li(
            s"${x.ldt}, username: ${x.username}, sevice: ${x.service}, message: ${x.message}"
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
      case e: ArithmeticException =>
        log.error(e.getMessage)
        Resume
      case e: NullPointerException =>
        log.error(e.getMessage)
        Restart
      case e: IllegalArgumentException =>
        log.error(e.getMessage)
        Stop
      case e: Exception =>
        log.error(e.getMessage)
        Escalate
    }

  def receive: PartialFunction[Any, Unit] = {
    case p: Props => sender() ! context.actorOf(p)
  }
}

private object Supervisor {
  def props(implicit system: ActorSystem, materializer: ActorMaterializer): Props = Props(new Supervisor)
}