import sbt._

object Dependencies {
  val akkaVersion = "2.4.17"
  val akkaHttpVersion = "10.0.7"
  val scalameterVersion = "0.8.2"
  val scalatestVersion = "3.0.1"
  val scalatagsVersion = "0.6.5"
  val scalacssVersion = "0.5.1"
  val json4sVersion = "3.5.2"
  val javaxMailApiVersion = "1.6.0-rc2"
  val leveldbVersion = "0.9"
  val leveldbjniVersion = "1.8"

  object Akka {
    val groupID = "com.typesafe.akka"
    val actor: ModuleID = groupID %% "akka-actor" % akkaVersion
    val http: ModuleID = groupID %% "akka-http" % akkaHttpVersion
    val httpTestKit: ModuleID = groupID %% "akka-http-testkit" % akkaHttpVersion % Test
    val httpSprayJson: ModuleID = groupID %% "akka-http-spray-json" % "10.0.7"
    val persistence: ModuleID = groupID %% "akka-persistence" % akkaVersion

    val all: Seq[ModuleID] = Seq(actor, http, httpTestKit, httpSprayJson, persistence)
  }

  val scalatest: ModuleID = "org.scalatest" %% "scalatest" % scalatestVersion % "test"

  val scalatags: ModuleID = "com.lihaoyi" %% "scalatags" % scalatagsVersion

  object Scalacss {
    val groupID = "com.github.japgolly.scalacss"
    val core: ModuleID = groupID %% "core" % scalacssVersion
    val ext: ModuleID = groupID %% "ext-scalatags" % scalacssVersion
    val all = Seq(core, ext)
  }

  val javaxMail: ModuleID = "com.sun.mail" % "javax.mail" % javaxMailApiVersion

  val config: ModuleID = "com.typesafe" % "config" % "1.3.1"

  object Leveldb {
    val leveldb: ModuleID = "org.iq80.leveldb"            % "leveldb"          % leveldbVersion
    val leveldbjni: ModuleID = "org.fusesource.leveldbjni"   % "leveldbjni-all"   % leveldbjniVersion
    val all = Seq(leveldb, leveldbjni)
  }

  val all: Seq[ModuleID] = Seq(
    scalatest,
    scalatags,
    javaxMail,
    config
  ) ++ Akka.all ++ Scalacss.all ++ Leveldb.all
}