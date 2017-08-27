package com.defender.api

import akka.actor.{ ActorRef, Props }
import akka.testkit.{ DefaultTimeout, TestKit }
import akka.util.Timeout
import com.defender.api.Persistence._
import com.defender.api.utils.IdGenerator

import scala.concurrent.{ ExecutionContext, Future }
import TestPersistentActor._
import akka.persistence.SnapshotMetadata

case class TestPersistentStateActor(storage: Seq[String] = Seq.empty) extends PersistentState[TestPersistentStateActor] {
  def create(x: String): TestPersistentStateActor = {
    if (storage contains x) throw new IllegalArgumentException
    copy(storage :+ x)
  }
  def exists(x: String): Boolean = storage contains x
  def getAll: Seq[String] = storage
  def updated(event: PersistentEvent): TestPersistentStateActor = event match {
    case CreatedEvt(x) => create(x)
  }
  def size: Int = storage.size
}

class TestPersistentActor(
    val id: String,
    val initState: TestPersistentStateActor
)(implicit val executor: ExecutionContext, val timeout: Timeout) extends PersistentStateActor[TestPersistentStateActor] {
  var snapshotCreated: Boolean = false

  def create(state: TestPersistentStateActor, x: String): Unit = {
    if (state.exists(x)) sender ! EntryAlreadyExists
    else {
      persist(CreatedEvt(x)) { event =>
        changeState(state.updated(event))
        sender ! Done
      }
    }
  }
  def findAll(state: TestPersistentStateActor): Unit =
    sender ! MultipleEntries(state.getAll)
  def behavior(state: TestPersistentStateActor): Receive = {
    case CreateCmd(x) => create(state, x)
    case GetAllCmd => findAll(state)
    case HasSnapshotCmd =>
      if (snapshotCreated) sender ! Yes else sender ! No
  }
  override def afterSnapshot(metadata: SnapshotMetadata, success: Boolean): Unit = {
    if (success) snapshotCreated = true
  }
}

object TestPersistentActor {
  case class CreateCmd(x: String) extends PersistentCommand
  case object GetAllCmd extends PersistentCommand
  case object HasSnapshotCmd extends PersistentCommand

  case class CreatedEvt(x: String) extends PersistentEvent

  sealed trait Response
  case object Done extends Response
  case object EntryAlreadyExists extends Response
  case object Yes
  case object No
  case class MultipleEntries(entries: Seq[String]) extends Response

  def props(id: String, state: TestPersistentStateActor = TestPersistentStateActor())(implicit executor: ExecutionContext, timeout: Timeout) =
    Props(new TestPersistentActor(id, state))
}

trait NewPersistentStateActor extends IdGenerator { _: TestKit with DefaultTimeout =>
  import system.dispatcher

  def newTestPersistentActor(
    state: TestPersistentStateActor = TestPersistentStateActor(),
    id: String = s"TestPersistentActor-$generateId"
  ): Future[ActorRef] = Future {
    system.actorOf(TestPersistentActor.props(id, state), id)
  }

  def generateString(): String = s"str-$generateId"
}
