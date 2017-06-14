package com.defender

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestKit
import com.defender.WatcherActor._

import scala.concurrent.Future

class WatcherActorSpec extends TestKit(ActorSystem("WatcherActorSpec")) with ActorBaseSpec {
  "A Watcher actor" when receive {
    "WatchRequest" must {
      "update state and send Notifier actor AddCommand" in {
        val f1 = generateRecords()
        val f2 = generateRecords()
        for {
          r1 <- f1
          r2 <- f2
          (watcherProbe, notifierProbe, watcher) <- newWatcher(r1, r2)
          w <- (watcher ? WatchRequest)(timeout, watcherProbe.ref)
          g <- (watcher ? GetRequest)(timeout, watcherProbe.ref)
            .mapTo[GetResponse]
            .map(_.records)
        } yield {
          w shouldBe WatchResponse
          g shouldBe r1 ++ r2
          notifierProbe.expectMsgType[NotifierActor.AddCommand].records shouldBe r2
        }
      }
    }
    "CleanRequest" must {
      "remove old records" in {
        val r1 = LogRecord(LocalDateTime.now(), "", "", "")
        val r2 = LogRecord(LocalDateTime.now().minusDays(1), "", "", "")
        val r3 = LogRecord(LocalDateTime.now().minusWeeks(1), "", "", "")
        val r4 = LogRecord(LocalDateTime.now().minusYears(1), "", "", "")
        val r = Set(r1, r2, r3, r4)
        for {
          (watcherProbe, _, watcher) <- newWatcher(r)
          c <- (watcher ? CleanRequest)(timeout, watcherProbe.ref)
          g <- (watcher ? GetRequest)(timeout, watcherProbe.ref)
            .mapTo[GetResponse]
            .map(_.records)
        } yield {
          c shouldBe CleanResponse
          g should contain only (r1, r2)
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
          (watcherProbe, _, watcher) <- newWatcher(r1)
          a <- (watcher ? AddCommand(r1))(timeout, watcherProbe.ref)
          r <- (watcher ? RemoveCommand(r1))(timeout, watcherProbe.ref)
          g <- (watcher ? GetRequest)(timeout, watcherProbe.ref)
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
          (watcherProbe, _, watcher) <- newWatcher(r1)
          rm1 <- (watcher ? RemoveCommand(r1.tail ++ r2))(timeout, watcherProbe.ref)
          rm2 <- (watcher ? RemoveCommand(r2))(timeout, watcherProbe.ref)
          g <- (watcher ? GetRequest)(timeout, watcherProbe.ref)
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