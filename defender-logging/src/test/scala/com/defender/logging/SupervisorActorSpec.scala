package com.defender.logging

import java.io.FileNotFoundException

import akka.actor.ActorSystem
import akka.actor.SupervisorStrategy.Resume
import akka.testkit.{ DefaultTimeout, ImplicitSender, TestActorRef, TestKit }
import com.defender.api.utils.{ AfterWords, IdGenerator, InMemoryCleanup, ShutdownAfterAll }
import com.defender.logging.utils.NewWatcherActor
import org.scalatest.{ AsyncWordSpecLike, Matchers }

class SupervisorActorSpec extends TestKit(ActorSystem("SupervisorActorSpec"))
    with AsyncWordSpecLike
    with DefaultTimeout
    with Matchers
    with ImplicitSender
    with LoggingConfig
    with IdGenerator
    with InMemoryCleanup
    with ShutdownAfterAll
    with NewWatcherActor
    with AfterWords {
  "A SupervisorActorSpec" when receive {
    "FileNotFoundException" must {
      "resume actor " in {
        val supervisor = TestActorRef[SupervisorActor](SupervisorActor.props)
        val strategy = supervisor.underlyingActor.supervisorStrategy.decider
        strategy(new FileNotFoundException) should be(Resume)
      }
    }
  }
}