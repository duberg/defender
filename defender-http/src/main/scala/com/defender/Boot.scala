package com.defender

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.defender.http.JsonSupport
import com.defender.http.routes.AuthLogServiceRoute
import com.defender.mail.MailSender
import com.defender.services.AuthLogService

import scala.concurrent.Future
import scala.concurrent.duration._

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
  val analyzer = LogAnalyzer(Configuration.Watchers.AuthLog.File)
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
  } println(s"Defender online at com.defender.http://localhost:$p/")

  def routes(notifierRef: ActorRef, watcherRef: ActorRef): Future[Route] = Future {
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
  def http(routes: Flow[HttpRequest, HttpResponse, Any]): Future[Int] = Future {
    val port = Configuration.Port
    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", port)
    port
  }
}
