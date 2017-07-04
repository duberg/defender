import com.typesafe.config.{Config, ConfigFactory}
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object BuildPlugin extends AutoPlugin {
  override def trigger = AllRequirements
  override def requires = JvmPlugin

  object autoImport {
    trait BuildEnv
    object BuildEnv {
      case object Production extends BuildEnv
      case object Development extends BuildEnv
      case object Docker extends BuildEnv
    }

    lazy val buildEnv = SettingKey[BuildEnv]("build-env", "Current build environment")
    lazy val confDir = SettingKey[File]("conf-dir", "Configuration directory")
    lazy val baseConfDir = SettingKey[File]("base-conf-dir", "Base configuration directory")
    lazy val envConfDir = SettingKey[File]("env-conf-dir", "Current configuration directory")
    lazy val envConfPath = SettingKey[File]("env-conf-path", "Current configuration path")
    lazy val conf  = SettingKey[Config]("conf", "Current configuration")
    lazy val build = TaskKey[File]("build", "Build project")
    lazy val compileScalastyle = TaskKey[Unit]("compileScalastyle")
    lazy val testAll = TaskKey[Unit]("test-all")
    lazy val dockerPublishLocalScript = TaskKey[Unit]("docker-publish-local-script", "Publishing custom docker image to localhost")
    lazy val dockerDebPath = SettingKey[String]("docker-deb-path", "Docker .deb package path")
    lazy val dockerPort = TaskKey[Int]("docker-port", "Docker port")
    lazy val dockerExecName = SettingKey[String]("docker-exec-name", "Docker exec name")
    lazy val scriptDir = TaskKey[File]("script-dir", "Script directory")
    lazy val genInstallScript = TaskKey[File]("gen-install-script", "Generate install script after build")

    lazy val itSettings = Defaults.itSettings ++ inConfig(IntegrationTest)(Seq(
      fork := false,
      parallelExecution := false,
      scalaSource := baseDirectory.value / "src/it/scala"
    ))
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    buildEnv := {
      sys.props.get("env")
        .orElse(sys.env.get("BUILD_ENV"))
        .flatMap {
          case "prod" => Some(BuildEnv.Production)
          case "dev" => Some(BuildEnv.Development)
          case "docker" => Some(BuildEnv.Docker)
        }
        .getOrElse(BuildEnv.Development)
    },
    onLoadMessage := {
      s"""|${onLoadMessage.value}
          |Running in build environment: ${buildEnv.value}
          |Current configuration: ${envConfPath.value}
          |""".stripMargin
    },
    confDir := baseDirectory.value / "conf",
    baseConfDir := confDir.value / "base",
    envConfDir := {
      buildEnv.value match {
        case BuildEnv.Production => confDir.value / "prod"
        case BuildEnv.Development => confDir.value / "dev"
        case BuildEnv.Docker => confDir.value / "docker"
      }
    },
    envConfPath := envConfDir.value / (buildEnv.value match {
        case BuildEnv.Production => "production.conf"
        case BuildEnv.Development => "development.conf"
        case BuildEnv.Docker => "docker.conf"
    }),
    conf := {
      val envConf = ConfigFactory.parseFile(envConfPath.value)
      val baseConfFile = baseConfDir.value / "base.conf"
      val baseConf = ConfigFactory.parseFile(baseConfFile)
      envConf.withFallback(baseConf).resolve()
    },
    scriptDir := baseDirectory.value / "scripts"
  )
}