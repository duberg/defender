package com.defender

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.defender.http.JsonSupport
import com.defender.http.routes.AuthLogServiceRoute
import com.defender.mail.MailSender
import com.defender.services.AuthLogService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object Boot extends App with JsonSupport {
  implicit val system = ActorSystem("defender", Configuration.AllConfig)
  implicit val materializer = ActorMaterializer()
  implicit val executor = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val supervisor = new Supervisor
  val notifierActorId = "notifier"
  val notifierActorProps = NotifierActor.props(
    notifierActorId,
    Configuration.Notifier.RetryInterval,
    new MailSender
  )
  val analyzer = new LogAnalyzer(Configuration.Watchers.AuthLog.File)
  val watcherActorId = "watcher"
  def watcherActorProps(notifier: ActorRef): Props = WatcherActor.props(
    watcherActorId,
    analyzer,
    notifier
  )

  for {
    notifierRef <- supervisor.add(notifierActorProps, notifierActorId)
    watcherRef <- supervisor.add(watcherActorProps(notifierRef), watcherActorId)
    r <- routes(notifierRef, watcherRef)
    p <- http(r)
  } println(s"Defender online at http://localhost:$p/")

  def routes(notifierRef: ActorRef, watcherRef: ActorRef) = Future {
    val watcher = new Watcher(
      watcherActorId,
      Configuration.Watchers.AuthLog.PollInterval,
      watcherRef,
      notifierRef,
      analyzer
    )
    val authLogService = new AuthLogService(watcher)
    val authLogServiceRoute = new AuthLogServiceRoute(authLogService)
    authLogServiceRoute.route
  }
  def http(routes: Flow[HttpRequest, HttpResponse, Any]) = Future {
    val port = Configuration.Port
    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", port)
    port
  }
}