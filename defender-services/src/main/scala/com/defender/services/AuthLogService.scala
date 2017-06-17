package com.defender.services

import com.defender.Implicits._
import com.defender.services.AuthLogService._
import com.defender.{ LogRecord, Watcher }

import scala.concurrent.{ ExecutionContext, Future }

class AuthLogService(watcher: Watcher)(implicit executor: ExecutionContext) {
  def records: Future[List[LogRecord]] =
    watcher.records.map { records =>
      records
        .takeRight(MaxRecords)
        .toList
        .sorted
    }
}

object AuthLogService {
  val MaxRecords = 30
}
