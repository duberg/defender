package com.defender.mail

import com.defender.LogRecord

import scalatags.Text.all._

class MailSender(handlerOpt: Option[NotificationException => Unit]) {
  def this() = this(None)
  def send(records: Seq[LogRecord]): Boolean = {
    if (records.nonEmpty) {
      try {
        Mail.send(
          "Defender: authentication failure event",
          html(
            body(
              ol(
                for (r <- records) yield li(
                  s"${r.localDateTime}, username: ${r.username}, sevice: ${r.service}, message: ${r.message}"
                )
              )
            )
          ).render
        )
        true
      } catch {
        case e: Throwable => handlerOpt match {
          case Some(handler) => handler(NotificationException(e.getMessage, e))
          case None => throw e
        }
      }
    }
    false
  }
  def withHandler(handler: NotificationException => Unit) = new MailSender(Option(handler))
}

case class NotificationException(message: String = "", cause: Throwable = None.orNull) extends Exception(message, cause)
