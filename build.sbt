lazy val root = (project in file("."))
  .settings(
    CommonSettings.commonSettings
  )

enablePlugins(JavaServerAppPackaging, SystemdPlugin)



    