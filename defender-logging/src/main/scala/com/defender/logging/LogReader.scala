package com.defender.logging

import java.io.File

import com.defender.logging.parsing.Parser
import resource._

import scala.collection.parallel.CollectionsHaveToParArray
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source

class LogReader(val file: File, val filter: LogFilter)(implicit executor: ExecutionContext) {
  def read: Future[LogEvents] = Future {
    import Parser.parse
    val lines = managed(Source.fromFile(file)).acquireAndGet(_.getLines().toParArray)
    val result = for {
      l <- lines
      p <- parse(l)
      if filter.matchAll(p)
    } yield p
    result.seq.toSet
  }
}

