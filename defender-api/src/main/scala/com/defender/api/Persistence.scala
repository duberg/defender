package com.defender.api

import java.time.{ Instant, LocalDateTime, ZoneId }

import akka.actor.ActorLogging
import akka.persistence._
import akka.util.Timeout

import scala.concurrent.ExecutionContext

object Persistence {
  val SnapshotInterval = 1000

  case object Persisted
  case object NotPersisted

  trait PersistentCommand

  trait PersistentEvent

  trait PersistentState[T] {
    def updated(event: PersistentEvent): T
  }

  /**
   * Актор в Функциональном стиле.
   *
   * При расширении в конструкторе дочернего актора указываем: (val id: String, val initState: State)
   * и реализуем метод def behavior(state: S): Receive
   *
   * Отсутствует shared mutable state.
   * Умеет посылать ответ на "ping".
   * Автоматически делает snapshot по заданному интервалу.
   *
   * @tparam T state type
   */
  trait PersistentStateActor[T <: PersistentState[T]] extends PersistentActor with ActorLogging with ActorLifecycleHooks {
    implicit def executor: ExecutionContext
    implicit def timeout: Timeout
    private var recoveryStateOpt: Option[T] = Option(initState)
    def id: String
    def initState: T
    def behavior(state: T): Receive
    def persistenceId: String = id
    def snapshotInterval: Int = SnapshotInterval
    def afterRecover(): Unit = {}
    def afterSnapshot(metadata: SnapshotMetadata, success: Boolean): Unit = {}
    def snapshot: Receive = {
      case m @ SaveSnapshotSuccess(SnapshotMetadata(pid, sequenceNr, timestamp)) =>
        log.info(s"New snapshot {{sequenceNr:$sequenceNr, ${d(timestamp)}}} saved")
        afterSnapshot(m.metadata, success = true)
      case m @ SaveSnapshotFailure(SnapshotMetadata(pid, sequenceNr, timestamp), reason) =>
        log.error(
          s"""Saving snapshot {{sequenceNr:$sequenceNr, ${d(timestamp)}}} failed
             |reason: $reason
           """.stripMargin
        )
        afterSnapshot(m.metadata, success = false)
    }
    def throwable: Receive = { case e: Exception => throw e }
    def echo: Receive = { case "ping" => sender() ! "pong" }
    def active(state: T): Receive = {
      behavior(state)
        .orElse(snapshot)
        .orElse(throwable)
        .orElse(echo)
        .andThen { _ =>
          if (lastSequenceNr % snapshotInterval == 0 && lastSequenceNr != 0) saveSnapshot(state)
        }
    }
    def changeState(state: T): Unit = context.become(active(state))
    def receiveCommand: Receive = active(initState)
    def recover: Receive = {
      case event: PersistentEvent =>
        recoveryStateOpt = recoveryStateOpt.map(_.updated(event))
        changeState(recoveryStateOpt.get)
      case SnapshotOffer(SnapshotMetadata(pid, sequenceNr, timestamp), snapshot: T @unchecked) =>
        recoveryStateOpt = Option(snapshot)
        changeState(snapshot)
        log.info(s"Snapshot {{sequenceNr:$sequenceNr, ${d(timestamp)}} offered")
      case RecoveryCompleted =>
        recoveryStateOpt = None
        log.info("Recovery completed")
    }
    def receiveRecover: Receive = recover.andThen(_ => afterRecover())
    def d(timestamp: Long): String =
      LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).toString
  }
}