package com.defender

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object Boot extends App with JsonSupport {
  implicit val system = ActorSystem("root", Configuration.AllConfig)
  implicit val materializer = ActorMaterializer()
  implicit val executor = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val defender = system.actorOf(SupervisorActor.props, "defender")
}

