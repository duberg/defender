package com.defender.notification

import java.util.Properties
import javax.mail._
import javax.mail.internet.{ InternetAddress, MimeMessage }

import akka.actor.{ ActorRef, Props }
import akka.util.Timeout
import com.defender.notification.Implicits._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scalatags.Text.all._

class MailNotifierActor(
    val id: String,
    val sources: Set[ActorRef],
    val retryInterval: FiniteDuration,
    val settings: MailSettings,
    val initState: NotifierActorState
)(implicit val executor: ExecutionContext, val timeout: Timeout) extends NotifierActor {
  def notifyRecipients(notifications: Notifications): Future[Notifications] = Future {
    if (notifications.nonEmpty) {
      send(
        "Defender event",
        html(
          body(
            for (x <- notifications.sorted) yield p(x.toString)
          )
        ).render
      )
    }
    notifications
  }
  def send(subject: String, message: String): Unit = {
    val props = new Properties()
    settings.smtp.foreach({ case (k, v) => props.put(k, v) })
    val session = Session.getInstance(
      props,
      new Authenticator {
        override def getPasswordAuthentication = new PasswordAuthentication(
          settings.username,
          settings.password
        )
      }
    )
    val msg = new MimeMessage(session)
    msg.setFrom(new InternetAddress(settings.from))
    msg.setRecipients(
      Message.RecipientType.TO,
      InternetAddress.parse(settings.to).asInstanceOf[Array[Address]]
    )
    msg.setSubject(subject)
    msg.setText(message, "utf-8", "html")
    Transport.send(msg)
  }
}

object MailNotifierActor extends {
  def props(
    id: String,
    sources: Set[ActorRef],
    retryInterval: FiniteDuration,
    settings: MailSettings,
    state: NotifierActorState = NotifierActorState()
  )(implicit executor: ExecutionContext, timeout: Timeout): Props =
    Props(new MailNotifierActor(id, sources, retryInterval, settings, state))
}

