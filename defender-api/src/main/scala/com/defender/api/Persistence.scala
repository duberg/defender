package com.defender.api

import java.time.{Instant, LocalDateTime, ZoneId}

import akka.actor.ActorLogging
import akka.persistence._
import akka.util.Timeout

import scala.concurrent.ExecutionContext

object Persistence {
  case object Persisted
  case object NotPersisted

  trait PersistentCommand

  trait PersistentEvent

  trait PersistentState[S] {
    def updated(event: PersistentEvent): S
  }

  trait PersistentStateActor[S <: PersistentState[S]] extends PersistentActor with ActorLogging with ActorLifecycleHooks {
    implicit def executor: ExecutionContext
    implicit def timeout: Timeout
    def id: String
    def persistenceId: String = id
    def snapShotInterval: Int = 100
    def initState: S
    def behavior(state: S): Receive
    def afterPersist(state: S): Receive = {
      case SaveSnapshotSuccess(SnapshotMetadata(pid, sequenceNr, timestamp)) =>
        log.info(s"New snapshot {{sequenceNr:$sequenceNr, ${d(timestamp)}}} saved")
    }
    def throwable: Receive = { case e: Exception => throw e }
    def active(state: S): Receive = {
      behavior(state)
        .orElse(afterPersist(state))
        .orElse(throwable)
        .andThen { _ =>
          if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
        }
    }
    def receiveCommand: Receive = active(initState)
    def receiveRecover: Receive = {
      case event: PersistentEvent =>
        context.become(active(initState.updated(event)))
        log.info("Persistent event replayed")
      case SnapshotOffer(SnapshotMetadata(pid, sequenceNr, timestamp), snapshot: S @unchecked) =>
        context.become(active(snapshot))
        log.info(s"Snapshot {{sequenceNr:$sequenceNr, ${d(timestamp)}} offered")
      case RecoveryCompleted =>
        log.info("Recovery completed")
    }
    def d(timestamp: Long): String =
      LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).toString
  }
}