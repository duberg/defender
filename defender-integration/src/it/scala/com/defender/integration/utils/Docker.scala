package com.defender.integration.utils

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.defender.integration._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.sys.process._

object Docker extends LazyLogging {
  private val port = new AtomicInteger(9090)
  def log = ProcessLogger(x => logger.info(x), x => logger.error(x))
  def logMessage(id: String, message: String): Unit = {
    logger.info(s"[${id.takeRight(12).dropRight(1)}] $message")
  }
  def runApp(containerId: String) = Future {
    val n = BuildInfo.dockerExecName
    s"docker exec $containerId $n" ! log
    logMessage(containerId, "Application in docker container started")
  }
  def run(port: Int): String = {
    val p = BuildInfo.dockerPort
    val n = BuildInfo.name
    val v = BuildInfo.version
    val id = s"docker run -d -p $port:$p $n:$v" !! log
    logMessage(id, "Docker container started")
    logMessage(id, s"Port mapping: $port -> $p")
    runApp(id)
    TimeUnit.SECONDS.sleep(1)
    id
  }
  def killAndRemove(containerId: String): Unit = {
    s"docker kill $containerId" ! log
    logMessage(containerId, "Docker container killed")
    s"docker rm $containerId" ! log
    logMessage(containerId, "Docker container removed")
  }
  def genLocalPort: Int = port.incrementAndGet()
}