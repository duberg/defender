import sbt.Keys._
import sbt._

import com.lightbend.sbt.JavaFormatterPlugin.JavaFormatterKeys
import com.typesafe.sbt.packager.Keys._

object CommonSettings {
  lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
    name := "defender",
    organization := "com.defender",
    version := "0.1.0-SNAPSHOT",
    maintainer := "Mark Duberg <scala@dr.com>",
    packageSummary := "Intrusion Detection System",
    packageDescription := "Intrusion Detection System for Ubuntu/Debian",
    scalaVersion := "2.12.2",
    libraryDependencies ++= Dependencies.all,
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    JavaFormatterKeys.javaFormattingSettingsFilename := "eclipse-java-google-style.xml",
    parallelExecution in Test := false
  )
}