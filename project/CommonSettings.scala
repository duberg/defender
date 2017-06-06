import sbt.Keys._
import sbt._

import com.typesafe.sbt.packager.Keys._

object CommonSettings {
  lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
    name := "defender",
    organization := "com.defender",
    version := "0.1.0-SNAPSHOT",
    maintainer := "Mark Duberg <scala@dr.com>",
    packageSummary := "Intrusion Detection System",
    packageDescription := "Intrusion Detection System for Ubuntu",
    scalaVersion := "2.12.2",
    libraryDependencies ++= Dependencies.all,
    parallelExecution in Test := false
  )
}