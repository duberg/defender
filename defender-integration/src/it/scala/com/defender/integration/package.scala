package com.defender

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

package object integration {
  implicit lazy val system: ActorSystem = ActorSystem("integration")
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
  implicit lazy val executor: ExecutionContextExecutor = system.dispatcher
  implicit lazy val timeout: Timeout = Timeout(5 seconds)
  lazy val http = Http(system)
}
