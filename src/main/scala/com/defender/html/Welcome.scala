package com.defender.html

import scalatags.Text.all._

object Welcome {
  def gen: String =
    "<!DOCTYPE html>" +
      html(
        cls := "no-touch base-layout",
        head(
          meta(name := "charset", value := "utf-8"),
          meta(name := "viewport", content := "width=device-width, initial-scale=1.0, user-scalable=no"),
        ),
        body(
          p("Defender is running")
        )
      ).render
}

