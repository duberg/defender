package com.defender

import java.time.LocalDateTime

object Implicits {
  implicit class RichLocalDateTime(ldt: LocalDateTime) {
    def >(ldt: LocalDateTime): Boolean = this.ldt.compareTo(ldt) > 1
    def <(ldt: LocalDateTime): Boolean = this.ldt.compareTo(ldt) <= -1
    def ==(ldt: LocalDateTime): Boolean = this.ldt.compareTo(ldt) == 0
  }
}

