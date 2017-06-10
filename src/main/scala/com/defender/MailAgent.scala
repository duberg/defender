package com.defender

import java.util.Properties
import javax.mail._
import javax.mail.internet.{ InternetAddress, MimeMessage }

object MailAgent {
  def send(subject: String, message: String): Unit = {
    val props = new Properties
    Configuration.Mail.Entries.foreach({ case (k, v) => props.put(k, v) })
    val session = Session.getInstance(
      props,
      new Authenticator {
        override def getPasswordAuthentication = new PasswordAuthentication(
          Configuration.Mail.Username,
          Configuration.Mail.Password
        )
      }
    )
    val msg = new MimeMessage(session)
    msg.setFrom(new InternetAddress(Configuration.Mail.From))
    msg.setRecipients(
      Message.RecipientType.TO,
      InternetAddress.parse(Configuration.Mail.To).asInstanceOf[Array[Address]]
    )
    msg.setSubject(subject)
    msg.setText(message, "utf-8", "html")
    try Transport.send(msg) catch {
      case e: Throwable => throw MailAgentException(e.getMessage, e)
    }
  }
}

case class MailAgentException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)