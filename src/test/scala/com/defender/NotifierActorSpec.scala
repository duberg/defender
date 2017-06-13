package com.defender

import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.defender.NotifierActor._
import com.defender.mail.{ MailSender, NotificationException }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, WordSpecLike }

import scala.concurrent.duration._

class NotifierActorSpec extends TestKit(ActorSystem("NotifierActorSpec"))
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

  def stubSender(records: Seq[LogRecord]): MailSender = {
    val sender = stub[MailSender]
    (sender.withHandler(_: NotificationException => Unit)).when(*).returns(sender)
    (sender.send _).when(records).returns(true)
    sender
  }

  "A Notifier actor" when receive {
    "NotifyRequest" must {
      "notify recipients and update state" in {
        val records = Seq(LogRecord(LocalDateTime.now(), "", "", ""))
        val state = NotifierActorState(records)
        val sender = stubSender(records)
        val notifier = system.actorOf(NotifierActor.props(id, 1 minute, sender, state))

        notifier ! NotifyRequest
        expectMsg(NotifyResponse)
        notifier ! GetRequest
        expectMsg(GetResponse(Seq.empty))
      }
    }
    "AddCommand" must {
      "update state" in {
        val records = Seq(LogRecord(LocalDateTime.now(), "", "", ""))
        val state = NotifierActorState(records)
        val sender = stubSender(records)
        val notifier = system.actorOf(NotifierActor.props(id, 1 minute, sender, state))

        notifier ! AddCommand(records)
        within(300 millisecond) {
          expectNoMsg(100 millisecond)
          awaitAssert(() => {
            notifier ! GetRequest
            expectMsg(GetResponse(Seq.empty))
          })
        }
      }
    }
  }
}