package com.defender.services

import com.defender.log.Event
import com.defender.watcher.AuthLogWatcher

import scala.concurrent.Future

class AuthLogService(sysLogWatcher: AuthLogWatcher) {
  def retrieveEvents: Future[Seq[Event]] = sysLogWatcher.retrieveEvents
}