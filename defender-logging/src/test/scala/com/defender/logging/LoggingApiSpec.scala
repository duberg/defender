package com.defender.logging

import akka.actor.ActorSystem
import akka.testkit.{ DefaultTimeout, ImplicitSender, TestKit }
import com.defender.api.utils.{ InMemoryCleanup, ShutdownAfterAll }
import com.defender.notification.Subscription._
import org.scalatest.Inspectors._
import org.scalatest.{ AsyncWordSpecLike, Matchers }

class LoggingApiSpec extends TestKit(ActorSystem("LoggingApiSpec"))
    with AsyncWordSpecLike
    with ImplicitSender
    with DefaultTimeout
    with Matchers
    with LoggingConfig
    with InMemoryCleanup
    with ShutdownAfterAll {
  "A LoggingApi" must {
    "start supervisor and all child actors" in {
      val api = LoggingApi("logging", loggingConfig)(system, system.dispatcher, timeout)
      api.watchers should not be empty
      api.watchers.foreach(_ ! Subscribe)
      forAll(receiveN(api.watchers.size))(_ shouldBe Subscribed)
    }
  }
}
