package com.defender.notification

import akka.actor.{ ActorRef, ActorSystem }
import akka.pattern.ask
import akka.testkit.{ DefaultTimeout, ImplicitSender, TestKit, TestProbe }
import com.defender.api.utils.{ InMemoryCleanup, ShutdownAfterAll }
import com.defender.notification.utils.SubscriptionActor
import org.scalatest.Inspectors._
import org.scalatest.{ AsyncWordSpecLike, Matchers }

class NotificationApiSpec extends TestKit(ActorSystem("LoggingApiSpec"))
    with AsyncWordSpecLike
    with ImplicitSender
    with DefaultTimeout
    with Matchers
    with NotificationConfig
    with InMemoryCleanup
    with ShutdownAfterAll {

  val probe = new TestProbe(system)
  probe.childActorOf(SubscriptionActor.props, "source1")
  probe.childActorOf(SubscriptionActor.props, "source2")

  "A NotificationApi" must {
    "find all sources refs using actorSelection" in {
      val api = NotificationApi("n1", notificationConfig)(system, system.dispatcher, timeout)
      probe.receiveN(2) shouldBe Seq(Subscription.Subscribe, Subscription.Subscribe)
    }
    "start supervisor and all child actors" in {
      val api = NotificationApi("n2", notificationConfig)(system, system.dispatcher, timeout)
      val supervisor = api.supervisor
      api.notifiers should not be empty
      api.notifiers.foreach(_ ! NotifierActor.GetRequest)
      forAll(receiveN(api.notifiers.size))(_ shouldBe a[NotifierActor.GetResponse])
      for {
        x <- ask(supervisor, SupervisorActor.SuperviseRequest(SubscriptionActor.props, "source3")).mapTo[SupervisorActor.SuperviseResponse].map(_.supervised)
      } yield x shouldBe a[ActorRef]
    }
  }
}