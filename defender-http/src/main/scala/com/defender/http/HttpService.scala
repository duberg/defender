package com.defender.http

import java.io.File

import akka.actor.ActorSystem
import akka.event.slf4j.SLF4JLogging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.defender.api.MessageMonitorActor
import com.defender.http.routes.LoggingServiceRoute
import com.defender.logging.LoggingApi
import com.defender.notification.NotificationApi
import com.defender.services.LoggingService
import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.duration._
import scala.io.StdIn

object HttpService extends SLF4JLogging {
  lazy val envConfig: Config = {
    val path = System.getProperty("config.env.file")
    log.info(s"Environment configuration file: $path")
    try ConfigFactory.parseFile(new File(path)) catch {
      case e: Exception =>
        log.error(s"Can't load config: ${e.getMessage}")
        ConfigFactory.load()
    }
  }
  lazy val config: Config = {
    val path = System.getProperty("config.base.file")
    val config = try ConfigFactory.parseFile(new File(path)) catch {
      case e: Exception =>
        log.error(s"Can't load config: ${e.getMessage}")
        ConfigFactory.load()
    }
    log.info(s"Base configuration file: $path")
    envConfig.withFallback(config)
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("defender", config)
    implicit val materializer = ActorMaterializer()
    implicit val executor = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    val logging = LoggingApi("logging", config)
    val notification = NotificationApi("notification", config)
    val loggingService = new LoggingService(logging)
    val loggingServiceRoute = new LoggingServiceRoute(loggingService)
    val port = config.getInt("defender.http.port")
    val routes = loggingServiceRoute.route
    val bindingFuture = Http().bindAndHandle(routes, "localhost", port)
    val messageMonitor = MessageMonitorActor()

    log.info(s"Http service is up http://localhost:$port")

    //    StdIn.readLine()
    //    bindingFuture
    //      .flatMap(_.unbind())
    //      .onComplete(_ => system.terminate())
  }
}
