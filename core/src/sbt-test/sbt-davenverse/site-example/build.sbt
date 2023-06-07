
ThisBuild / crossScalaVersions := Seq("2.12.18", "2.13.6")

lazy val `site-example` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .aggregate(core)

lazy val core = project.in(file("core"))
  .settings(name := "site-example")

lazy val site = project.in(file("site"))
  .enablePlugins(DavenverseMicrositePlugin)
  .disablePlugins(MimaPlugin)
  .dependsOn(core)
