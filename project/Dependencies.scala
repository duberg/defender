import sbt._

object Dependencies {
  val akkaVersion = "2.4.17"
  val akkaHttpVersion = "10.0.7"
  val scalameterVersion = "0.8.2"
  val scalatestVersion = "3.0.1"
  val scalamockVersion = "3.5.0"
  val scalatagsVersion = "0.6.5"
  val scalacssVersion = "0.5.1"
  val json4sVersion = "3.5.2"
  val javaxMailApiVersion = "1.6.0-rc2"
  val leveldbVersion = "0.9"
  val leveldbjniVersion = "1.8"

  object Akka {
    val groupID = "com.typesafe.akka"
    val actor: ModuleID = groupID %% "akka-actor" % akkaVersion
    val testKit: ModuleID = groupID %% "akka-testkit" % akkaVersion
    val http: ModuleID = groupID %% "akka-http" % akkaHttpVersion
    val httpTestKit: ModuleID = groupID %% "akka-http-testkit" % akkaHttpVersion % Test
    val httpSprayJson: ModuleID = groupID %% "akka-http-spray-json" % "10.0.7"
    val persistence: ModuleID = groupID %% "akka-persistence" % akkaVersion
    val slf4j: ModuleID = groupID %% "akka-slf4j" % akkaVersion
    val logback: ModuleID = "ch.qos.logback" % "logback-classic" % "1.2.3"
    val persistenceInmemory: ModuleID = "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.4.18.1"
    val all: Seq[ModuleID] = Seq(actor, http, httpTestKit, httpSprayJson, persistence, slf4j, logback, persistenceInmemory)
  }

  object Scalacss {
    val groupID = "com.github.japgolly.scalacss"
    val core: ModuleID = groupID %% "core" % scalacssVersion
    val ext: ModuleID = groupID %% "ext-scalatags" % scalacssVersion
    val all = Seq(core, ext)
  }

  object Leveldb {
    val leveldb: ModuleID = "org.iq80.leveldb"            % "leveldb"          % leveldbVersion
    val leveldbjni: ModuleID = "org.fusesource.leveldbjni"   % "leveldbjni-linux64"   % leveldbjniVersion
    val all = Seq(leveldb, leveldbjni)
  }

  val scalatest: ModuleID = "org.scalatest" %% "scalatest" % scalatestVersion % Test
  val scalamock: ModuleID = "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test
  val scalatags: ModuleID = "com.lihaoyi" %% "scalatags" % scalatagsVersion
  val javaxMail: ModuleID = "com.sun.mail" % "javax.mail" % javaxMailApiVersion
  val config: ModuleID = "com.typesafe" % "config" % "1.3.1"

  val all: Seq[ModuleID] = Seq(
    scalatest,
    scalamock,
    scalatags,
    javaxMail,
    config
  ) ++ Akka.all ++ Scalacss.all ++ Leveldb.all
}