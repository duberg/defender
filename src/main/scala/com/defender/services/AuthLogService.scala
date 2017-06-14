package com.defender.services

import com.defender.{ Records, Watcher }

import scala.concurrent.{ ExecutionContext, Future }

class AuthLogService(watcher: Watcher)(implicit executor: ExecutionContext) {
  def records: Future[Records] = watcher.records.map(_.takeRight(30))
}