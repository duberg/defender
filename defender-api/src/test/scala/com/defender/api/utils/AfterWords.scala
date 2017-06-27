package com.defender.api.utils

import org.scalatest._

trait AfterWords { self: AsyncWordSpecLike =>
  def provide: AfterWord = afterWord("provide")
  def receive: AfterWord = afterWord("receive")
}
