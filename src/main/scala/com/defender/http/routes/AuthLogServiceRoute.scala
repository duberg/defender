package com.defender.http.routes

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives.{ complete, get, onSuccess, pathEndOrSingleSlash }
import akka.http.scaladsl.server.Route
import com.defender.services.AuthLogService

import scala.concurrent.{ ExecutionContext, Future }
import scalatags.Text.all._

class AuthLogServiceRoute(authLogService: AuthLogService)(implicit executor: ExecutionContext) {
  def events: Future[String] = authLogService.records.map(events =>
    html(
      body(
        ol(
          for (x <- events.toSeq) yield li(
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
