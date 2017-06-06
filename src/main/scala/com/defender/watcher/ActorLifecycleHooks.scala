package com.defender.watcher

import akka.actor.{ Actor, ActorLogging }

trait ActorLifecycleHooks { self: Actor with ActorLogging =>
  override def preStart(): Unit = log.info("created")
  override def postStop(): Unit = log.info("terminated")
}
