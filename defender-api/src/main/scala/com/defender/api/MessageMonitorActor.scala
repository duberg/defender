package com.defender.api

import akka.actor._

class MessageMonitorActor extends Actor with ActorLogging {
  def receive: Receive = {
    case UnhandledMessage(message: Any, sender: ActorRef, recipient: ActorRef) =>
      log.error(
        s"""UnhandledMessage:
           |  message: $message
           |  sender: $sender
           |  recipient: $recipient""".stripMargin
      )
    case DeadLetter(message: Any, sender: ActorRef, recipient: ActorRef) =>
      log.warning(
        s"""DeadLetter:
           |  message: $message
           |  sender: $sender
           |  recipient: $recipient""".stripMargin
      )
  }
}

object MessageMonitorActor {
  def props: Props = Props[MessageMonitorActor]
  def apply()(implicit system: ActorSystem): ActorRef = {
    val monitor = system.actorOf(props, "message-monitor")
    system.eventStream.subscribe(monitor, classOf[DeadLetter])
    system.eventStream.subscribe(monitor, classOf[UnhandledMessage])
    monitor
  }
}

