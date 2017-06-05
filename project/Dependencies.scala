import sbt._

object Dependencies {
  val akkaVersion = "2.4.17"
  val akkaHttpVersion = "10.0.7"
  val scalameterVersion = "0.8.2"
  val scalatestVersion = "3.0.1"
  val scalatagsVersion = "0.6.5"
  val scalacssVersion = "0.5.1"

  object Akka {
    val groupID = "com.typesafe.akka"
    val actor = groupID %% "akka-actor" % akkaVersion
    val http = groupID %% "akka-http" % akkaHttpVersion
    val httpTestKit = groupID %% "akka-http-testkit" % akkaHttpVersion % Test
    val persistence = groupID %% "akka-persistence" % akkaVersion
    val all = Seq(actor, http, httpTestKit)
  }

  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % "test"

  val scalatags = "com.lihaoyi" %% "scalatags" % scalatagsVersion

  object Scalacss {
    val groupID = "com.github.japgolly.scalacss"
    val core = groupID %% "core" % scalacssVersion
    val ext = groupID %% "ext-scalatags" % scalacssVersion
    val all = Seq(core, ext)
  }

  val all = Seq(
    scalatest,
    scalatags
  ) ++ Akka.all ++ Scalacss.all
}