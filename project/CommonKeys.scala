import com.typesafe.config.Config
import sbt._

object CommonKeys {
  val confDir: SettingKey[File] = Def.settingKey[File]("Configuration directory")
  val appBaseConfDir: SettingKey[File] = Def.settingKey[File]("Application base configuration directory")
  val appEnvConfDir: SettingKey[File] = Def.settingKey[File]("Application environment configuration directory")
  val appConf: SettingKey[Config] = Def.settingKey[Config]("Application configuration")

  val build: TaskKey[File] = TaskKey[File]("build", "Build project")
}
