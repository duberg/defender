package com.defender.utils

import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorRef
import akka.testkit.{ DefaultTimeout, TestKit, TestKitBase, TestProbe }
import com.defender._
import com.defender.mail.{ MailSender, NotificationException }
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

trait ActorBaseSpec extends AsyncWordSpecLike
    with BeforeAndAfterAll
    with DefaultTimeout
    with AsyncMockFactory
    with Matchers
    with Base { self: TestKitBase =>
  private val id = new AtomicInteger(0)

  def generateId: String = s"a${id.incrementAndGet()}"

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  def receive: AfterWord = afterWord("receive")

  def stubMailSender(sendResult: Boolean = true): MailSender = {
    def send = {
      if (sendResult) Future { Option(Set.empty[LogRecord]) }
      else Future { None }
    }
    val s = stub[MailSender]
    (s.withHandler(_: NotificationException => Unit)).when(*).returns(s)
    (s.send _).when(*).returns(send)
    s
  }

  def stubAnalyzerProcess(processResult: Records): LogAnalyzer = {
    def process = Future { processResult }
    val s = stub[LogAnalyzer]
    (s.process _).when().returns(process)
    s
  }

  def generateRecords(min: Int = 1, max: Int = 10): Future[Records] = Future {
    val random = new Random()
    val n: Int = random.nextInt(max + 1 - min) + min
    (for (i <- 1 to n) yield {
      val id = generateId
      LogRecord(
        LocalDateTime.now().minusDays(i),
        s"user-$id",
        s"service-$id",
        ""
      )
    }).toSet
  }

  def newNotifier(state: Records = Set.empty, sendResult: Boolean = true): Future[(TestProbe, ActorRef)] = Future {
    val probe = new TestProbe(system)
    val id = generateId
    val notifier = probe.childActorOf(NotifierActor.props(id, 1 minute, stubMailSender(sendResult), NotifierActorState(state)), id)
    (probe, notifier)
  }

  def newWatcher(state: Records, stubAnalyzerProcessResult: Records): Future[(TestProbe, TestProbe, ActorRef)] = Future {
    val watcherProbe = new TestProbe(system)
    val notifierProbe = new TestProbe(system)
    val id = generateId
    val watcher = system.actorOf(WatcherActor.props(id, stubAnalyzerProcess(stubAnalyzerProcessResult), notifierProbe.ref, WatcherActorState(state)), id)
    (watcherProbe, notifierProbe, watcher)
  }

  def newWatcher(state: Records): Future[(TestProbe, TestProbe, ActorRef)] = Future {
    val watcherProbe = new TestProbe(system)
    val notifierProbe = new TestProbe(system)
    val id = generateId
    val analyzer = LogAnalyzer(file)
    val watcher = system.actorOf(WatcherActor.props(id, analyzer, notifierProbe.ref, WatcherActorState(state)), id)
    (watcherProbe, notifierProbe, watcher)
  }
}
