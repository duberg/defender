package com.defender

import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import com.defender.WatcherActor._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, WordSpecLike }

class WatcherActorSpec extends TestKit(ActorSystem("WatcherActorSpec"))
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

  "A Watcher actor" when receive {
    "WatchRequest" must {
      "update state and send Notifier actor AddCommand" in {
        val probe = TestProbe()
        val r = Seq(LogRecord(LocalDateTime.now(), "", "", ""))
        val analyzer = stub[LogAnalyzer]
        val watcher = system.actorOf(WatcherActor.props(id, analyzer, probe.ref))

        (analyzer.analyze _).when().returns(r)

        watcher ! WatchRequest
        expectMsg(WatchResponse)
        watcher ! GetRequest
        expectMsg(GetResponse(r))
        probe.expectMsg(NotifierActor.AddCommand(r))
      }
    }
    "CleanRequest" must {
      "update state" in {
        val probe = TestProbe()
        val r1 = LogRecord(LocalDateTime.now(), "", "", "")
        val r2 = LogRecord(LocalDateTime.now().minusDays(1), "", "", "")
        val r3 = LogRecord(LocalDateTime.now().minusWeeks(1), "", "", "")
        val r4 = LogRecord(LocalDateTime.now().minusYears(1), "", "", "")
        val r = Seq(r1, r2, r3, r4)
        val f = new File(getClass.getResource("/auth.log").getFile)
        val analyzer = new LogAnalyzer(f)
        val watcher = system.actorOf(WatcherActor.props(id, analyzer, probe.ref))

        watcher ! AddCommand(r)
        watcher ! CleanRequest
        expectMsg(CleanResponse)
        watcher ! GetRequest
        expectMsg(GetResponse(Seq(r1, r2)))
        probe.expectMsgType[NotifierActor.AddCommand]
      }
    }
  }
}