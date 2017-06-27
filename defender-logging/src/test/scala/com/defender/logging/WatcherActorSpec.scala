package com.defender.logging

import java.io.FileNotFoundException

import akka.actor.ActorSystem
import akka.pattern.{ ask, pipe }
import akka.testkit.{ DefaultTimeout, ImplicitSender, TestKit }
import com.defender.api.Persistence._
import com.defender.api.utils._
import com.defender.logging.WatcherActor._
import com.defender.logging.utils.NewWatcherActor
import com.defender.notification._
import org.scalatest.Inspectors._
import org.scalatest._

class WatcherActorSpec extends TestKit(ActorSystem("WatcherActorSpec"))
    with AsyncWordSpecLike
    with ImplicitSender
    with DefaultTimeout
    with Matchers
    with InMemoryCleanup
    with ShutdownAfterAll
    with NewWatcherActor
    with AfterWords {
  "A WatcherActor" when receive {
    "Watch" must {
      "read log file and update state" in {
        val (subscriber, _) = newWatcherActor()
        subscriber.expectMsgType[Subscription.Event].notifications should not be empty
      }
      "send notification to all subscribers" in {
        val (subscribers, _) = newWatcherActorWithSubscribers()
        forAll(subscribers)(_.expectMsgType[Subscription.Event].notifications should not be empty)
      }
    }
    "AddCommand" must {
      "must not persist event with same log events" in {
        val logEvents = generateLogEvents()
        val (_, watcher) = newWatcherActorWithState(logEvents)
        for {
          x <- ask(watcher, AddCommand(logEvents))
          y <- ask(watcher, RemoveCommand(logEvents))
          z <- ask(watcher, GetRequest)
            .mapTo[GetResponse]
            .map(_.logEvents)
        } yield {
          x shouldBe NotPersisted
          y shouldBe Persisted
          z shouldBe empty
        }
      }
    }
    "RemoveCommand" must {
      "must not persist event with already log events" in {
        val logEvents1 = generateLogEvents(min = 2)
        val logEvents2 = generateLogEvents()
        val (_, watcher) = newWatcherActorWithState(logEvents1)
        for {
          x <- ask(watcher, RemoveCommand(logEvents1.tail ++ logEvents2))
          y <- ask(watcher, RemoveCommand(logEvents2))
          z <- ask(watcher, GetRequest).mapTo[GetResponse].map(_.logEvents)
        } yield {
          x shouldBe Persisted
          y shouldBe NotPersisted
          z should contain only logEvents1.head
        }
      }
    }
    "Sweep" must {
      "remove old log events" in {
        val (subscriber, watcher) = newWatcherActor()
        subscriber.expectMsgType[Subscription.Event].notifications should not be empty
        for {
          x <- ask(watcher, Sweep) pipeTo watcher
          y <- ask(watcher, GetRequest).mapTo[GetResponse].map(_.logEvents)
        } yield {
          x shouldBe a[RemoveCommand]
          y shouldBe empty
        }
      }
    }
    "FileChangeCommand" must {
      s"throw FileNotFoundException exception" in {
        val (_, watcher) = newWatcherActorWithFileNotFoundException()
        for (x <- (ask(watcher, FileChangeCommand(1L)) pipeTo watcher).mapTo[Throwable])
          yield x shouldBe a[FileNotFoundException]
      }
    }
  }
}
