package com.defender.notification

import akka.actor.ActorSystem
import akka.pattern.{ ask, pipe }
import akka.testkit.{ DefaultTimeout, ImplicitSender, TestKit, TestProbe }
import com.defender.api.Persistence._
import com.defender.api.utils.{ AfterWords, InMemoryCleanup, ShutdownAfterAll }
import com.defender.notification.NotifierActor._
import com.defender.notification.utils.NewNotifierActor
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Inspectors._
import org.scalatest.{ AsyncWordSpecLike, Matchers }

import scala.concurrent.Future

class NotifierActorSpec extends TestKit(ActorSystem("NotifierActorSpec"))
    with AsyncWordSpecLike
    with ImplicitSender
    with DefaultTimeout
    with Matchers
    with InMemoryCleanup
    with ShutdownAfterAll
    with NewNotifierActor
    with AfterWords
    with AsyncMockFactory {
  "A NotifierActor" when receive {
    "Subscription.Subscribed" must {
      "send Subscription.Subscribe to all sources" in {
        val sources = (for (s <- 5 to 10) yield new TestProbe(system)).toSet
        newNotifierActor(sources)
        forAll(sources)(_.expectMsg(Subscription.Subscribe) shouldBe Subscription.Subscribe)
      }
    }
    "NotifyRequest" must {
      "notify recipients and update state" in {
        val notifications = generateNotifications()
        val notifyRecipients = stubFunction[Notifications, Future[Notifications]]
        notifyRecipients.when(notifications).returns(Future.successful(notifications))
        val (_, notifier) = newNotifierActor(notifications, notifyRecipients)
        for {
          x <- ask(notifier, Notify) pipeTo notifier
          y <- ask(notifier, GetRequest).mapTo[GetResponse].map(_.notifications)
        } yield {
          notifyRecipients.verify(notifications)
          x shouldBe RemoveCommand(notifications)
          y shouldBe empty
        }
      }
    }
    "Subscription.Event" must {
      "update state with new notifications" in {
        val notifications = generateNotifications()
        val subscriptionEvent = Subscription.newEvent(notifications)
        val (_, notifier) = newNotifierActor()
        for {
          x <- ask(notifier, subscriptionEvent) pipeTo notifier
          y <- ask(notifier, GetRequest).mapTo[GetResponse].map(_.notifications)
        } yield {
          x shouldBe Subscription.Received(subscriptionEvent)
          y shouldBe notifications
        }
      }
    }
    "RemoveCommand" must {
      "remove notifications" in {
        val n1 = generateNotifications()
        val n2 = generateNotifications()
        val n3 = generateNotifications()
        val (_, notifier) = newNotifierActor(n1 ++ n2)
        for {
          x <- ask(notifier, RemoveCommand(Seq(n1.head)))
          y <- ask(notifier, RemoveCommand(n3))
          z <- ask(notifier, GetRequest).mapTo[GetResponse].map(_.notifications)
        } yield {
          x shouldBe Persisted
          y shouldBe NotPersisted
          z should contain only (n1.tail ++ n2: _*)
        }
      }
      "must not persist event with already removed records" in {
        val n1 = generateNotifications(min = 2)
        val n2 = generateNotifications()
        val (_, notifier) = newNotifierActor(n1)
        for {
          x <- ask(notifier, RemoveCommand(n1.tail ++ n2))
          y <- ask(notifier, RemoveCommand(n2))
          z <- ask(notifier, GetRequest).mapTo[GetResponse].map(_.notifications)
        } yield {
          x shouldBe Persisted
          y shouldBe NotPersisted
          z should contain only n1.head
        }
      }
    }
    "AddCommand" must {
      "add notifications" in {
        val n1 = generateNotifications()
        val n2 = generateNotifications()
        val n3 = generateNotifications()
        val (_, notifier) = newNotifierActor(n1 ++ n2)
        for {
          x <- ask(notifier, AddCommand(n3))
          y <- ask(notifier, GetRequest).mapTo[GetResponse].map(_.notifications)
        } yield {
          x shouldBe Notify
          y should contain allElementsOf (n1 ++ n2 ++ n3)
        }
      }
    }
  }
}