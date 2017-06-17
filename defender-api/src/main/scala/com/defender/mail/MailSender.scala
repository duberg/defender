package com.defender.mail

import akka.util.Timeout
import com.defender.Implicits._
import com.defender.Records

import scala.concurrent.{ ExecutionContext, Future }
import scalatags.Text.all._

class MailSender(handlerOpt: Option[NotificationException => Unit])(implicit executor: ExecutionContext, timeout: Timeout) {
  def this()(implicit executor: ExecutionContext, timeout: Timeout) = this(None)
  def send(records: Records): Future[Option[Records]] = Future {
    if (records.nonEmpty) {
      val sorted = records.toSeq.sorted
      try {
        MailTransport.send(
          "Defender: authentication failure event",
          html(
            body(
              ol(
                for (r <- sorted) yield li(
                  s"${r.localDateTime}, username: ${r.username}, sevice: ${r.service}, message: ${r.message}"
                )
              )
            )
          ).render
        )
        Option(records)
      } catch {
        case e: Throwable => handlerOpt match {
          case Some(handler) =>
            handler(NotificationException(e.getMessage, e))
            None
          case None => throw e
        }
      }
    } else None
  }
  def withHandler(handler: NotificationException => Unit): MailSender = new MailSender(Option(handler))
}

case class NotificationException(message: String = "", cause: Throwable = None.orNull) extends Exception(message, cause)
