package com.defender

import java.time.LocalDateTime

case class Event(ldt: LocalDateTime, username: String, service: String, message: String)