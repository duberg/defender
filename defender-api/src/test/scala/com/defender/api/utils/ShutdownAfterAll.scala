package com.defender.api.utils

import akka.testkit.TestKit
import org.scalatest.{ BeforeAndAfterAll, Suite }

trait ShutdownAfterAll extends BeforeAndAfterAll { _: Suite with TestKit =>
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}
