package com.defender.api

import java.io.File

import com.typesafe.config.Config

import scala.collection.JavaConverters._
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.util.matching.Regex

trait ModuleConfig {
  implicit class RichConfig(val underlying: Config) extends ModuleConfig {
    def getFile(path: String): File = new File(underlying.getString(path))
    def getRegex(path: String): Regex = underlying.getString(path).r
    def getStringSet(path: String): Set[String] = underlying.getStringList(path).asScala.toSet
    def getSet[T](path: String, map: String => T): Set[T] = underlying.getStringList(path).asScala.map(map).toSet
    def getScalaDuration(path: String): Duration = Duration(underlying.getString(path))
    def getFiniteDuration(path: String): FiniteDuration = {
      val d = getScalaDuration(path)
      Option(d).collect({ case d: FiniteDuration => d }).get
    }
  }
}
