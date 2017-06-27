package com.defender

import java.time.LocalDateTime

import akka.actor.ActorRef

package object notification {
  case class Notification(localDateTime: LocalDateTime, message: String, fields: Set[(String, String)]) {
    override def toString: String = {
      val b = new StringBuilder(s"$localDateTime,")
      fields.foreach({ case (k, v) => b ++= s" {{$k: $v}}," })
      b ++= s" {{message: $message}}"
      b.toString()
    }
  }

  type Subscribers = Set[ActorRef]
  type Notifications = Seq[Notification]
}

