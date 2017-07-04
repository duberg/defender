import sbt._

object Dependencies {
  val currentVersion = "0.3.1"
  val akkaVersion = "2.5.3"
  val akkaHttpVersion = "10.0.8"
  val akkaPersistenceInmemoryVersion = "2.5.1.1"
  val logbackVersion = "1.2.3"
  val akkaKryoVersion = "0.5.1"
  val minlogVersion = "1.2"
  val leveldbVersion = "0.9"
  val leveldbjniVersion = "1.8"
  val scalatestVersion = "3.0.1"
  val scalamockVersion = "3.5.0"
  val scalatagsVersion = "0.6.5"
  val scalacssVersion = "0.5.1"
  val javaxmailVersion = "1.6.0-rc2"
  val typesafeConfigVersion = "1.3.1"
  val scalaArmVersion = "2.0"
  val scalaLoggingVersion = "3.6.0"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaStreamTestKit = "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % akkaVersion
  val akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  val akkaPersistenceInmemory = "com.github.dnvriend" %% "akka-persistence-inmemory" % akkaPersistenceInmemoryVersion % Test
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % akkaVersion
  val akkaKryo = "com.github.romix.akka" %% "akka-kryo-serialization" % akkaKryoVersion

  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion

  val minlog = "com.esotericsoftware.minlog" % "minlog" % minlogVersion

  val leveldb = "org.iq80.leveldb" % "leveldb" % leveldbVersion
  val leveldbjni = "org.fusesource.leveldbjni" % "leveldbjni-linux64" % leveldbjniVersion

  val scalacssCore = "com.github.japgolly.scalacss" %% "core" % scalacssVersion
  val scalacssExt = "com.github.japgolly.scalacss" %% "ext-scalatags" % scalacssVersion

  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion % Test

  val scalamock = "org.scalamock" %% "scalamock-scalatest-support" % scalamockVersion % Test

  val scalatags = "com.lihaoyi" %% "scalatags" % scalatagsVersion

  val javaxmail = "com.sun.mail" % "javax.mail" % javaxmailVersion

  val typesafeConfig = "com.typesafe" % "config" % typesafeConfigVersion

  val scalaArm = "com.jsuereth" %% "scala-arm" % scalaArmVersion

  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
}