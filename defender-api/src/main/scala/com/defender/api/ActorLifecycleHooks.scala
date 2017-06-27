package com.defender.api

import akka.actor.{ Actor, ActorLogging }

trait ActorLifecycleHooks { self: Actor with ActorLogging =>
  override def preStart(): Unit = log.info("Actor created")
  override def postStop(): Unit = log.info("Actor terminated")
}
