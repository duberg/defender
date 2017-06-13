import CommonConfigurations._
import sbt.Keys._
import sbt._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport.Linux
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport.packageTemplateMapping
import CommonKeys._
import com.typesafe.config.{Config, ConfigFactory}

object CommonSettings {
  lazy val developmentSettings = Seq(
    appEnvConfDir := confDir.value / "local"
  )

  lazy val productionSettings = Seq(
    appEnvConfDir := confDir.value / "prod"
  )

  lazy val commonSettings: Seq[Def.Setting[_]] = {
    inConfig(Development)(developmentSettings) ++
      inConfig(Production)(productionSettings) ++
      Seq(
        name := "defender",
        organization := "com.defender",
        version := "0.1.4-SNAPSHOT",
        maintainer := "Mark Duberg <scala@dr.com>",
        packageSummary := "Intrusion Detection System",
        packageDescription := "Intrusion Detection System for Ubuntu",
        daemonUser in Linux := name.value,
        daemonGroup in Linux := "adm", // group with read rights /var/log/auth.log
        scalaVersion := "2.12.2",
        libraryDependencies ++= Dependencies.all,
        parallelExecution in Test := true,
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
        javaOptions in Universal ++= Seq(
          // -J params will be added as jvm parameters
          s"-Dconfig.base.file=${defaultLinuxInstallLocation.value}/${name.value}/etc/base.conf",
          s"-Dconfig.env.file=${defaultLinuxInstallLocation.value}/${name.value}/etc/production.conf",
          s"-Dlogback.configurationFile=${defaultLinuxInstallLocation.value}/${name.value}/etc/logback.xml"
        )
        //fork := true
      )
  }
}