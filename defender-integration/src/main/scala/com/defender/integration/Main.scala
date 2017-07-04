package com.defender.integration

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging

object Main extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("started")
    // Auto close docker container
    TimeUnit.MINUTES.sleep(5)
    logger.info("exit")
  }
}
