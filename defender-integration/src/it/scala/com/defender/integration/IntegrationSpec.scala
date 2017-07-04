package com.defender.integration

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import com.defender.api.utils.AfterWords
import com.defender.integration.utils.DockerBeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{AsyncWordSpecLike, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class IntegrationSpec extends AsyncWordSpecLike
  with Matchers
  with AfterWords
  with Eventually
  with IntegrationPatience
  with DockerBeforeAndAfterAll {
  "An http service" when {
    "/" must responseWithCode {
      "200" in {
        eventually(timeout(5 seconds), interval(1 second)) {
          val f = http.singleRequest(HttpRequest(uri = endpoint)).map(_.status)
          Await.result(f, 100 milliseconds) shouldBe StatusCodes.OK
        }
      }
    }
  }
}
