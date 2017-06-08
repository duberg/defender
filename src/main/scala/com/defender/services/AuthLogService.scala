package com.defender.services

import com.defender.{ Event, Watcher }

import scala.concurrent.{ ExecutionContext, Future }

class AuthLogService(watcher: Watcher)(implicit executor: ExecutionContext) {
  def events: Future[Seq[Event]] = watcher.event.map(_.takeRight(30))
}