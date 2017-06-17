import Dependencies._
import com.typesafe.config._

lazy val confDir: SettingKey[File] = settingKey[File]("Configuration directory")
lazy val appBaseConfDir: SettingKey[File] = settingKey[File]("Application base configuration directory")
lazy val appEnvConfDir: SettingKey[File] = settingKey[File]("Application environment configuration directory")
lazy val appConf: SettingKey[Config] = settingKey[Config]("Application configuration")
lazy val prepareBuild: TaskKey[Unit] = TaskKey[Unit]("prepareBuild", "Prepare build project")
lazy val build: TaskKey[File] = TaskKey[File]("build", "Build project")
lazy val compileScalastyle: TaskKey[Unit] = taskKey[Unit]("compileScalastyle")

lazy val Development: Configuration = config("development") extend Universal describedAs "scope to build production packages"
lazy val Production: Configuration = config("production") extend Debian describedAs "scope to build development packages"

lazy val developmentSettings = Seq(
  appEnvConfDir := confDir.value / "local",
  build := {
    prepareBuild.value
    packageBin.value
  }
)

lazy val productionSettings = Seq(
  appEnvConfDir := confDir.value / "prod",
  build := {
    prepareBuild.value
    packageBin.value
  }
)

lazy val commonSettings: Seq[Def.Setting[_]] = {
    Seq(
      organization := "com.defender",
      version := "0.2.0-SNAPSHOT",
      scalaVersion := "2.12.2",
      logLevel := Level.Warn,
      resolvers ++= Seq(
        Resolver.typesafeRepo("releases"),
        Resolver.sonatypeRepo("releases")
      ),
      parallelExecution in Test := true,
      scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-feature",
        "-language:postfixOps"
      ),
      compileScalastyle := (scalastyle in Compile).toTask("").value,
      javaOptions += "-Xmx4G",
      fork := true
    )
}

lazy val rootSettings: Seq[Def.Setting[_]] = {
  commonSettings ++
    inConfig(Development)(developmentSettings) ++
    inConfig(Production)(productionSettings) ++
    Seq(
      name := "defender",
      maintainer := "Mark Duberg <scala@dr.com>",
      packageSummary := "Intrusion Detection System",
      packageDescription := "Intrusion Detection System for Ubuntu",
      mainClass in Compile := Some("com.defender.Boot"),
      daemonUser in Linux := name.value,
      daemonGroup in Linux := "adm", // group with read rights /var/log/auth.log
      defaultLinuxConfigLocation in Production := "/usr/share/defender/etc",
      mappings in Universal ++= {
        Seq(
          (appBaseConfDir.value / "base.conf") -> s"${defaultLinuxConfigLocation.value}/base.conf",
          ((appEnvConfDir in Production).value / "production.conf") -> s"${defaultLinuxConfigLocation.value}/production.conf",
          ((appEnvConfDir in Production).value / "logback.xml") -> s"${defaultLinuxConfigLocation.value}/logback.xml"
        )
      },
      // Add an empty folder to mappings
      linuxPackageMappings ++= {
        Seq(
          packageTemplateMapping(appConf.value.getString("akka.persistence.journal.leveldb.dir"))()
            .withUser(name.value)
            .withGroup("adm"),
          packageTemplateMapping(appConf.value.getString("akka.persistence.snapshot-store.local.dir"))()
            .withUser(name.value)
            .withGroup("adm")
        )
      },
      confDir := baseDirectory.value / "conf",
      appBaseConfDir := confDir.value / "base",
      appConf := {
        def configuration: Config = {
          val envConf: Config = {
            val f = (appEnvConfDir in Production).value / "production.conf"
            println(s"Environment configuration file: $f")
            ConfigFactory.parseFile(f)
          }
          val conf: Config = {
            val f = appBaseConfDir.value / "base.conf"
            val c = ConfigFactory.parseFile(f)
            println(s"Base configuration file: $f")
            envConf.withFallback(c).resolve()
          }
          conf
        }
        configuration
      },
      javaOptions in Universal ++= Seq(
        // -J params will be added as jvm parameters
        s"-Dconfig.base.file=${defaultLinuxInstallLocation.value}/${name.value}/etc/base.conf",
        s"-Dconfig.env.file=${defaultLinuxInstallLocation.value}/${name.value}/etc/production.conf",
        s"-Dlogback.configurationFile=${defaultLinuxInstallLocation.value}/${name.value}/etc/logback.xml"
      ),
      prepareBuild := {
        taskKeyAll(test in Test).all(allProjectsFilter).value
        taskKeyAll(compileScalastyle).all(allProjectsFilter).value
      }
    )
}

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  defenderApi,
  defenderServices,
  defenderHttp,
  defenderTest
)

lazy val allProjectsFilter = ScopeFilter(
  inProjects(aggregatedProjects: _*),
  inConfigurations(Compile)
)

def defenderModule(name: String): Project =
  Project(id = name, base = file(name))
    .settings(commonSettings)

lazy val root = Project(
  id = "defender",
  base = file("."),
  aggregate = aggregatedProjects,
  settings = rootSettings,
  configurations = Seq(Development, Production)
).enablePlugins(
  JavaServerAppPackaging ,
  LinuxPlugin,
  UniversalPlugin,
  DebianPlugin,
  SystemdPlugin
)

lazy val defenderApi = defenderModule("defender-api")
  .settings(libraryDependencies ++= Seq(
    akkaActor,
    akkaHttp,
    akkaHttpSprayJson,
    akkaPersistence,
    akkaSlf4j,
    logback,
    leveldb,
    leveldbjni,
    scalatags,
    javaxmail,
    typesafeConfig
  ))

lazy val defenderServices = defenderModule("defender-services")
  .dependsOn(defenderApi)

lazy val defenderHttp = defenderModule("defender-http")
  .dependsOn(defenderServices)
  .settings(libraryDependencies ++= Seq(
    akkaActor,
    akkaHttp,
    akkaHttpSprayJson,
    scalatags,
    typesafeConfig
  ))

lazy val defenderTest = defenderModule("defender-test")
  .dependsOn(defenderHttp)
  .settings(libraryDependencies ++= Seq(
    akkaActor,
    akkaHttp,
    akkaHttpSprayJson,
    akkaPersistence,
    akkaSlf4j,
    logback,
    javaxmail,
    typesafeConfig,
    akkaTestKit,
    akkaHttpTestKit,
    akkaPersistenceInmemory,
    scalatest,
    scalamock
  ))