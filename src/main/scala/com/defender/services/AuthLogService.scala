package com.defender.services

import com.defender.{ LogRecord, Watcher }

import scala.concurrent.{ ExecutionContext, Future }

class AuthLogService(watcher: Watcher)(implicit executor: ExecutionContext) {
  def events: Future[Seq[LogRecord]] = watcher.event.map(_.takeRight(30))
}