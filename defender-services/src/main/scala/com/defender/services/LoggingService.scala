package com.defender.services

import akka.event.slf4j.SLF4JLogging
import com.defender.logging.Implicits._
import com.defender.logging.{ LogEvent, LoggingApi }

import scala.concurrent.{ ExecutionContext, Future }

class LoggingService(logging: LoggingApi)(implicit executor: ExecutionContext) extends SLF4JLogging {
  def logEvents: Future[Seq[LogEvent]] =
    logging.logEvents.map { logEvent =>
      logEvent
        .takeRight(30)
        .sorted
    }
}
