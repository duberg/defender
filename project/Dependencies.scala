import sbt._

object Dependencies {
  val akkaVersion = "2.4.17"
  val akkaHttpVersion = "10.0.7"
  val scalatestVersion = "3.0.1"
  val scalamockVersion = "3.5.0"
  val scalatagsVersion = "0.6.5"
  val scalacssVersion = "0.5.1"
  val javaxmailVersion = "1.6.0-rc2"
  val leveldbVersion = "0.9"
  val leveldbjniVersion = "1.8"
  val typesafeConfigVersion = "1.3.1"

  val akkaActor: ModuleID = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaTestKit: ModuleID = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
  val akkaHttp: ModuleID = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaHttpTestKit: ModuleID = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  val akkaHttpSprayJson: ModuleID = "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.7"
  val akkaPersistence: ModuleID = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
  val akkaSlf4j: ModuleID = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaPersistenceInmemory: ModuleID = "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.4.18.1" % Test

  val logback: ModuleID = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val scalacssCore: ModuleID = "com.github.japgolly.scalacss" %% "core" % scalacssVersion
  val scalacssExt: ModuleID = "com.github.japgolly.scalacss" %% "ext-scalatags" % scalacssVersion

  val leveldb: ModuleID = "org.iq80.leveldb" % "leveldb" % leveldbVersion
  val leveldbjni: ModuleID = "org.fusesource.leveldbjni" % "leveldbjni-linux64" % leveldbjniVersion

  val scalatest: ModuleID = "org.scalatest" %% "scalatest" % scalatestVersion % Test

  val scalamock: ModuleID = "org.scalamock" %% "scalamock-scalatest-support" % scalamockVersion % Test

  val scalatags: ModuleID = "com.lihaoyi" %% "scalatags" % scalatagsVersion

  val javaxmail: ModuleID = "com.sun.mail" % "javax.mail" % javaxmailVersion

  val typesafeConfig: ModuleID = "com.typesafe" % "config" % typesafeConfigVersion
}