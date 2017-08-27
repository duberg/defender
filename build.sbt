import Dependencies._
import com.typesafe.sbt.packager.docker._

lazy val commonSettings: Seq[Def.Setting[_]] = {
    Seq(
      organization := "com.defender",
      version := {
        buildEnv.value match {
          case BuildEnv.Production => currentVersion
          case env => s"$currentVersion-${env.toString.toUpperCase}"
        }
      },
      scalaVersion := "2.12.3",
      logLevel := Level.Info,
      resolvers ++= Seq(
        Resolver.typesafeRepo("releases"),
        Resolver.sonatypeRepo("releases")
      ),
      scalacOptions ++= Seq(
        "-unchecked",
        "-deprecation",
        "-feature",
        "-language:postfixOps",
        "-language:implicitConversions"
      ),
      compileScalastyle := (scalastyle in Compile).toTask("").value,
      javaOptions += "-Xmx4G",
      parallelExecution in Test := true,
      testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-W", "10", "5"),
      fork := true
    )
}

lazy val dockerSettings: Seq[Def.Setting[_]] = {
  Seq(
    //dockerBaseImage := "ubuntu"
  ) ++ inConfig(Docker)(Seq(
    dockerDebPath := {
      val f = defaultLinuxInstallLocation.value
      val n = (name in root).value
      val v = (version in root).value
      s"$f/lib/$n-$v.deb"
    },
    mainClass in Compile := Some("com.defender.integration.Main"),
    dockerPackageMappings ++= {
      val log = streams.value.log
      val f = (packageBin in (root, Debian)).value
      log.success("Build .deb success")
      Seq(f -> dockerDebPath.value)
    },
    daemonUser := "root",
    dockerCommands ++= Seq(
      //Cmd("EXPOSE", conf.value.getString("defender.http.port")),
      //ExecCmd("RUN", "apt-get", "update"),
      ExecCmd("RUN", "dpkg", "-i", dockerDebPath.value))
  ))
}

lazy val debianSettings: Seq[Def.Setting[_]] = inConfig(Debian)(Seq(
  debianPackageDependencies in Debian ++= Seq("java2-runtime", "bash (>= 2.05a-11)"),
  linuxPackageMappings ++= Seq(
    packageTemplateMapping(conf.value.getString("akka.persistence.journal.leveldb.dir"))()
      .withUser(name.value)
      .withGroup("adm"),
    packageTemplateMapping(conf.value.getString("akka.persistence.snapshot-store.local.dir"))()
      .withUser(name.value)
      .withGroup("adm")
  )
))

lazy val universalSettings: Seq[Def.Setting[_]] = inConfig(Universal)(Seq(
  javaOptions := {
    val d = s"${defaultLinuxInstallLocation.value}/${name.value}/conf"
    Seq(
      s"-Dconfig.base.file=$d/base.conf",
      s"-Dconfig.env.file=$d/env.conf",
      s"-Dlogback.configurationFile=$d/logback.xml"
    )
  },
  mappings ++= {
    val to = "conf"
    def envMappings: Seq[(File, String)] = {
      val from = envConfDir.value
      buildEnv.value match {
        case BuildEnv.Production => Seq(
          (from / "production.conf") -> s"$to/env.conf",
          (from / "logback.xml") -> s"$to/logback.xml"
        )
        case BuildEnv.Development => Seq(
          (from / "development.conf") -> s"$to/env.conf",
          (from / "logback-test.xml") -> s"$to/logback.xml"
        )
        case BuildEnv.Docker => Seq(
          (from / "docker.conf") -> s"$to/env.conf",
          (from / "logback-test.xml") -> s"$to/logback.xml"
        )
      }
    }
    ((baseConfDir.value / "base.conf") -> s"$to/base.conf") +: Seq(envMappings: _*)
  }
))

lazy val rootSettings: Seq[Def.Setting[_]] = {
  val settings = Seq(
    name := "defender",
    daemonUser in Linux := name.value,
    daemonGroup in Linux := "adm", // group with read rights /var/log/auth.log
    maintainer := "Mark Duberg <mduberg@yahoo.com>",
    packageSummary := "Intrusion Detection System",
    packageDescription := "Intrusion Detection System for Ubuntu",
    mainClass in Compile := Some("com.defender.http.HttpService"),
    build := Def.sequential(
      taskKeyAll(compileScalastyle).all(allProjectsFilter),
      compileScalastyle in defenderIntegration,
      taskKeyAll(packageBin in Compile).all(allProjectsFilter),
      scalariformFormat in (defenderIntegration, Compile),
      packageBin in Debian,
      genInstallScript
    ).value,
    aggregate in build := false,
    testAll := Def.sequential(
      dockerPublishLocalScript in defenderIntegration,
      taskKeyAll(test in Test).all(allProjectsFilter),
      test in (defenderIntegration, IntegrationTest)
    ).value,
    aggregate in testAll := false,
    genInstallScript := {
      val env = buildEnv.value match {
        case BuildEnv.Production => "prod"
        case BuildEnv.Development => "dev"
        case BuildEnv.Docker => "docker"
      }
      val f = scriptDir.value / s"install-$env"
      val n = s"${name.value}_${version.value}_all.deb"
      val c =
        s"""#!/usr/bin/env bash
          |cd "$$(dirname "$$0")"/..
          |sudo dpkg -i target/$n
        """.stripMargin
      IO.write(f, c)
      f
    }
  )
  commonSettings ++
    universalSettings ++
    debianSettings ++
    settings ++
    itSettings
}

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  defenderApi,
  defenderNotification,
  defenderLogging,
  defenderServices,
  defenderHttp
)

lazy val allProjectsFilter = ScopeFilter(
  inProjects(aggregatedProjects: _*)
  //inConfigurations(Compile)
)

def projectModule(name: String): Project = Project(
  id = name,
  base = file(name),
  settings = commonSettings ++ itSettings
)

lazy val root = Project(
  id = "defender",
  base = file("."),
  settings = rootSettings,
  dependencies = Seq(
    defenderApi,
    defenderNotification,
    defenderLogging,
    defenderServices,
    defenderHttp
  ),
  aggregate = aggregatedProjects
).enablePlugins(
  BuildPlugin,
  JavaServerAppPackaging,
  UniversalPlugin,
  LinuxPlugin,
  DebianPlugin,
  SystemdPlugin
)

lazy val defenderApi = projectModule("defender-api")
  .settings(libraryDependencies ++= Seq(
    akkaActor,
    akkaStream,
    akkaPersistence,
    akkaPersistenceQuery,
    akkaSlf4j,
    akkaTestKit,
    akkaStreamTestKit,
    akkaPersistenceInmemory,
    akkaRemote,
    akkaKryo,
    logback,
    leveldb,
    leveldbjni,
    minlog,
    scalatags,
    javaxmail,
    typesafeConfig,
    scalatest,
    scalamock
  ))

lazy val defenderLogging = projectModule("defender-logging")
  .dependsOn(defenderNotification % "test->test;compile->compile")
  .settings(libraryDependencies ++= Seq(
    scalaArm
  ))

lazy val defenderNotification = projectModule("defender-notification")
  .dependsOn(defenderApi % "test->test;compile->compile")
  .settings(libraryDependencies ++= Seq(
    javaxmail
  ))

lazy val defenderServices = projectModule("defender-services")
  .dependsOn(defenderLogging)

lazy val defenderHttp = projectModule("defender-http")
  .dependsOn(defenderServices)
  .settings(libraryDependencies ++= Seq(
    akkaHttp,
    akkaHttpSprayJson
  ))

lazy val integrationSettings: Seq[Def.Setting[_]]  = {
  val settings = Seq(
    dockerPublishLocalScript := {
      val log = streams.value.log
      val p = ((baseDirectory in root).value / "scripts/docker-publish-local.sh").getPath
      log.info(s"Run $p")
      if (p ! log == 0) log.success("Publishing custom docker image success")
      else throw new RuntimeException("Publishing custom docker image failed")
    },
    dockerExecName := (name in root).value,
    dockerPort := (conf in root).value.getInt("defender.http.port"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, dockerExecName, dockerPort),
    buildInfoPackage := "com.defender.integration",
    libraryDependencies ++= Seq(
      scalaLogging,
      akkaHttp,
      akkaHttpSprayJson
    )
  )
  settings ++ dockerSettings
}

lazy val defenderIntegration = projectModule("defender-integration")
  .dependsOn(defenderApi % "test->test;compile->compile;it->test")
  .configs(IntegrationTest, Docker)
  .settings(integrationSettings)
  .enablePlugins(
    BuildInfoPlugin,
    JavaAppPackaging,
    DockerPlugin
  )