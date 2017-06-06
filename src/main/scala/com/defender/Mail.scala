package com.defender

import java.util.Properties
import javax.mail._
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object Mail {
  def send(subject: String, message: String): Boolean = {
    val props = new Properties
    Configuration.mail.toMap.foreach({ case (k, v) => props.put(k, v) })
    val session = Session.getInstance(
      props,
      new Authenticator {
        override def getPasswordAuthentication = new PasswordAuthentication(
          Configuration.mail.username,
          Configuration.mail.password
        )
      }
    )
    val msg = new MimeMessage(session)
    msg.setFrom(new InternetAddress(Configuration.mail.from))
    msg.setRecipients(
      Message.RecipientType.TO,
      InternetAddress.parse(Configuration.mail.to).asInstanceOf[Array[Address]]
    )
    msg.setSubject(subject)
    msg.setText(message, "utf-8", "html")
    try Transport.send(msg) catch {
      case e: MessagingException => false
    }
    true
  }
}