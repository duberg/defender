package com.defender.integration.utils

import org.scalatest.{BeforeAndAfterAll, Suite}

trait DockerBeforeAndAfterAll extends BeforeAndAfterAll { _: Suite =>
  var containerId: String = _
  var endpoint: String = _

  override def beforeAll(): Unit = {
    val p = Docker.genLocalPort
    containerId = Docker.run(p)
    endpoint = s"http://localhost:$p"
  }
  override def afterAll(): Unit = {
    Docker.killAndRemove(containerId)
  }
}
