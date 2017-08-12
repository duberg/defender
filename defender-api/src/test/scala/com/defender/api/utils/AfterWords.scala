package com.defender.api.utils

import org.scalatest._

trait AfterWords { self: AsyncWordSpecLike =>
  def provide: AfterWord = afterWord("provide")
  def receive: AfterWord = afterWord("receive")
  def response: AfterWord = afterWord("response")
  def responseWithCode: AfterWord = afterWord("response with http code")
}
