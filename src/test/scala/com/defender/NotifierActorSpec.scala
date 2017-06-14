package com.defender

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestKit
import com.defender.NotifierActor._

class NotifierActorSpec extends TestKit(ActorSystem("NotifierActorSpec")) with ActorBaseSpec {
  "A Notifier actor" when receive {
    "NotifyRequest" must {
      "notify recipients and update state" in {
        val f = generateRecords()
        for {
          r <- f
          (probe, notifier) <- newNotifier(r)
          _ <- (notifier ? NotifyRequest)(timeout, probe.ref)
          g <- (notifier ? GetRequest)(timeout, probe.ref)
            .mapTo[GetResponse]
            .map(_.records)
        } yield {
          g shouldBe empty
        }
      }
    }
    "RemoveCommand" must {
      "update state" in {
        val f1 = generateRecords()
        val f2 = generateRecords()
        val f3 = generateRecords()
        for {
          r1 <- f1
          r2 <- f2
          r3 <- f3
          (probe, notifier) <- newNotifier(r1 ++ r2, sendResult = false)
          rm1 <- (notifier ? RemoveCommand(Set(r1.head)))(timeout, probe.ref)
          rm2 <- (notifier ? RemoveCommand(r3))(timeout, probe.ref)
          g <- (notifier ? GetRequest)(timeout, probe.ref)
            .mapTo[GetResponse]
            .map(_.records)
        } yield {
          rm1 shouldBe Persisted
          rm2 shouldBe NotPersisted
          g should not contain r1.head
        }
      }
    }
    "AddCommand" must {
      "must not persist event with same records" in {
        val f1 = generateRecords()
        val f2 = generateRecords()
        for {
          r1 <- f1
          r2 <- f2
          (probe, notifier) <- newNotifier(r1, sendResult = false)
          a <- (notifier ? AddCommand(r1))(timeout, probe.ref)
          r <- (notifier ? RemoveCommand(r1))(timeout, probe.ref)
          g <- (notifier ? GetRequest)(timeout, probe.ref)
            .mapTo[GetResponse]
            .map(_.records)
        } yield {
          a shouldBe NotPersisted
          r shouldBe Persisted
          g shouldBe empty
        }
      }
      "must not persist event with already removed records" in {
        val f1 = generateRecords(min = 2)
        val f2 = generateRecords()
        for {
          r1 <- f1
          r2 <- f2
          (probe, notifier) <- newNotifier(r1, sendResult = false)
          rm1 <- (notifier ? RemoveCommand(r1.tail ++ r2))(timeout, probe.ref)
          rm2 <- (notifier ? RemoveCommand(r2))(timeout, probe.ref)
          g <- (notifier ? GetRequest)(timeout, probe.ref)
            .mapTo[GetResponse]
            .map(_.records)
        } yield {
          rm1 shouldBe Persisted
          rm2 shouldBe NotPersisted
          g should contain only r1.head
        }
      }
    }
  }
}