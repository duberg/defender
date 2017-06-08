package com.defender

import java.time.LocalDateTime

case class Event(localDateTime: LocalDateTime, username: String, service: String, message: String)