package com.defender.log

import java.time.LocalDateTime

case class Event(localDateTime: LocalDateTime, username: String, service: String, message: String)