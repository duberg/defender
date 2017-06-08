package com.defender

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.defender.services.AuthLogService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scalatags.Text.all._

object Defender extends App with JsonSupport {
  implicit val system = ActorSystem("defender")
  implicit val materializer = ActorMaterializer()
  implicit val executor = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val notifierActor = system.actorOf(NotifierActor.props("default", Configuration.Server.Notifier.RetryInterval), "notifier-default")
  val watcher = new Watcher(
    Configuration.Server.Watchers.AuthLog.Name,
    Configuration.Server.Watchers.AuthLog.File,
    Configuration.Server.Watchers.AuthLog.MatchPattern,
    Configuration.Server.Watchers.AuthLog.PollInterval,
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

  val port = Configuration.Server.Port
  val bindingFuture = Http().bindAndHandle(route, "localhost", port)

  println(s"Defender online at http://localhost:$port/")
}