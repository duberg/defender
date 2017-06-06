package com.defender

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.defender.services.AuthLogService
import com.defender.watcher._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scalatags.Text.all._

object Defender extends App with JsonSupport {
  implicit val system = ActorSystem("defender")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val authLogWatcher = new AuthLogWatcher
  val authLogService = new AuthLogService(authLogWatcher)

  def retrieveEvents: Future[String] = authLogService.retrieveEvents.map(events =>
    html(
      body(
        ol(
          for (e <- events) yield li(
            s"${e.localDateTime}, username: ${e.username}, sevice: ${e.service}, message: ${e.message}"
          )
        )
      )
    ).render)

  val route: Route = {
    pathEndOrSingleSlash {
      get {
        onSuccess(retrieveEvents) { events =>
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, events))
        }
      }
    }
  }

  val port = Configuration.server.port
  val bindingFuture = Http().bindAndHandle(route, "localhost", port)

  println(s"Defender online at http://localhost:$port/")
}