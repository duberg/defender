package com.defender

import java.time.LocalDateTime

case class LogRecord(localDateTime: LocalDateTime, username: String, service: String, message: String)
