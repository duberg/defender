import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt._

object CommonConfigurations {
  lazy val Development: Configuration = config("development") extend Universal describedAs "scope to build production packages"
  lazy val Production: Configuration = config("production") extend Debian describedAs "scope to build development packages"
}
