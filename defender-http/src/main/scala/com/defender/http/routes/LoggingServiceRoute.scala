package com.defender.http.routes

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives.{ complete, get, onSuccess, pathEndOrSingleSlash }
import akka.http.scaladsl.server.Route
import com.defender.services.LoggingService

import scala.concurrent.{ ExecutionContext, Future }
import scalatags.Text.all._

class LoggingServiceRoute(loggingService: LoggingService)(implicit executor: ExecutionContext) {
  def events: Future[String] = loggingService.logEvents.map(logEvents =>
    html(
      body(
        ol(
          for (x <- logEvents) yield li(
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
}
