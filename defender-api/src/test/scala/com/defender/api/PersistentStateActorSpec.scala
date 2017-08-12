package com.defender.api

import akka.actor.{ ActorSystem, Kill, PoisonPill }
import akka.testkit.{ DefaultTimeout, TestKit }
import com.defender.api.utils.{ AfterWords, InMemoryCleanup, ShutdownAfterAll }
import org.scalatest.{ AsyncWordSpecLike, Matchers }
import akka.pattern.ask

import scala.concurrent.Future

class PersistentStateActorSpec extends TestKit(ActorSystem("PersistentSpec"))
    with AsyncWordSpecLike
    with DefaultTimeout
    with Matchers
    with InMemoryCleanup
    with ShutdownAfterAll
    with NewPersistentStateActor
    with AfterWords {
  import Persistence._
  import TestPersistentActor._
  "A PersistentStateActor" when receive {
    "ping" must response {
      "pong" in {
        for {
          a <- newTestPersistentActor()
          x <- ask(a, "ping")
        } yield x shouldBe "pong"
      }
    }
    "GetAllCmd" must {
      "return all elements from init state" in {
        val s = Seq(generateString(), generateString())
        for {
          a <- newTestPersistentActor(TestPersistentStateActor(s))
          x <- ask(a, GetAllCmd).mapTo[MultipleEntries].map(_.entries)
        } yield x shouldBe s
      }
    }
    "CreateCmd" must {
      "change state" in {
        val ss = Seq(generateString(), generateString())
        val s1 = generateString()
        for {
          a <- newTestPersistentActor(TestPersistentStateActor(ss))
          x <- ask(a, CreateCmd(s1))
          y <- ask(a, GetAllCmd).mapTo[MultipleEntries].map(_.entries)
        } yield {
          x shouldBe Done
          y should contain(s1)
        }
      }
    }
    "create snapshot" in {
      val n = SnapshotInterval + SnapshotInterval / 2
      for {
        a <- newTestPersistentActor()
        _ <- Future.sequence {
          for (i <- 1 to n) yield ask(a, CreateCmd(generateString()))
        }
        x <- ask(a, GetAllCmd).mapTo[MultipleEntries].map(_.entries)
        z <- ask(a, HasSnapshotCmd)
      } yield {
        x should have size n
        z shouldBe Yes
      }
    }
    "restore from snapshot" in {
      val id = s"TestRecovering-$generateId"
      val n = SnapshotInterval + SnapshotInterval / 2
      for {
        a <- newTestPersistentActor(id = id)
        _ <- Future.sequence {
          for (i <- 1 to n) yield ask(a, CreateCmd(generateString()))
        }
        x <- ask(a, GetAllCmd).mapTo[MultipleEntries].map(_.entries)
        y <- ask(a, HasSnapshotCmd)
        _ <- Future { a ! PoisonPill }
        _ <- Future { Thread.sleep(500) }
        c <- newTestPersistentActor(id = id)
        z <- ask(c, GetAllCmd).mapTo[MultipleEntries].map(_.entries)
      } yield {
        x should have size n
        y shouldBe Yes
        z should have size n
      }
    }
  }
}