import CommonConfigurations._

lazy val root = (project in file("."))
  .enablePlugins(
    JavaServerAppPackaging ,
    LinuxPlugin,
    UniversalPlugin,
    DebianPlugin,
    SystemdPlugin
  )
  .configs(Development, Production)
  // define custom settings
  .settings(CommonSettings.commonSettings)

