package com.defender.logging

import java.time.LocalDateTime

case class LogEvent(
  localDateTime: LocalDateTime,
  username: String,
  service: String,
  message: String
)
