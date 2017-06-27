package com.defender.api.utils

import java.util.concurrent.atomic.AtomicInteger

import com.defender.api.utils.IdGenerator.id

trait IdGenerator {
  def generateId: String = s"a${id.incrementAndGet()}"
}
object IdGenerator {
  private val id = new AtomicInteger(0)
}
