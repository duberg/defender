package com.defender

import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ ActorRef, ActorSystem }
import akka.testkit.{ DefaultTimeout, TestKit, TestKitBase, TestProbe }
import com.defender.mail.{ MailSender, NotificationException }
import org.scalamock.MockFactoryBase
import org.scalatest._

import scala.util.Random
import org.scalamock.scalatest.{ AsyncMockFactory, MockFactory }

import scala.concurrent.Future
import scala.concurrent.duration._

trait ActorBaseSpec extends AsyncWordSpecLike
    with BeforeAndAfterAll
    with DefaultTimeout
    with AsyncMockFactory
    with Matchers { self: TestKitBase =>
  private val id = new AtomicInteger(0)
  def generateId: String = s"a${id.incrementAndGet()}"

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  def receive: AfterWord = afterWord("receive")

  def stubSender(sendResult: Boolean = true): MailSender = {
    def r = {
      if (sendResult) Future { Option(Set.empty[LogRecord]) }
      else Future { None }
    }
    val s = stub[MailSender]
    (s.withHandler(_: NotificationException => Unit)).when(*).returns(s)
    (s.send _).when(*).returns(r)
    s
  }

  def stubAnalyzer(analyzeResult: Records): LogAnalyzer = {
    def r = Future { Option(analyzeResult) }
    val s = stub[LogAnalyzer]
    (s.process _).when().returns(r)
    s
  }

  def generateRecords(min: Int = 1, max: Int = 10): Future[Records] = Future {
    val random = new Random()
    val n: Int = random.nextInt(max + 1 - min) + min
    (for (i <- 1 to n) yield {
      val id = generateId
      LogRecord(
        LocalDateTime.now().minusHours(i),
        s"user-$id",
        s"service-$id",
        ""
      )
    }).toSet
  }

  def newNotifier(state: Records = Set.empty, sendResult: Boolean = true): Future[(TestProbe, ActorRef)] = Future {
    val probe = new TestProbe(system)
    val id = generateId
    val notifier = probe.childActorOf(NotifierActor.props(id, 1 minute, stubSender(sendResult), NotifierActorState(state)), id)
    (probe, notifier)
  }

  def newWatcher(state: Records, analyzeResult: Records = Set.empty): Future[(TestProbe, TestProbe, ActorRef)] = Future {
    val watcherProbe = new TestProbe(system)
    val notifierProbe = new TestProbe(system)
    val id = generateId
    val watcher = system.actorOf(WatcherActor.props(id, stubAnalyzer(analyzeResult), notifierProbe.ref, WatcherActorState(state)), id)
    (watcherProbe, notifierProbe, watcher)
  }
}
