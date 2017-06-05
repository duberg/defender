package com.defender

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.defender.html.Welcome

object Defender extends App {
  implicit val system = ActorSystem("defender")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val route: Route = {
    pathEndOrSingleSlash {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, Welcome.gen))
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8181)

  println(s"Defender online at http://localhost:8181/")
}