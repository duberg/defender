package com.defender

import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import com.defender.WatcherActor._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, WordSpecLike }

class SupervisorActorSpec extends TestKit(ActorSystem("SupervisorActorSpec"))
    with WordSpecLike
    with ImplicitSender
    with BeforeAndAfterAll
    with MockFactory {
  private val testActorId = new AtomicInteger(0)
  def id: String = testActorId.incrementAndGet().toString

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  def receive: AfterWord = afterWord("receive")

  "A Supervisor actor" when {
    "supervised actor failed" must {
      "restart actor" in {

      }
    }
    "supervised actor failed with IOException" must {
      "resume actor" in {
        // todo get error from templ file delete
      }
    }
  }
}