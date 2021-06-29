# sbt-davenverse - Shared Configs and Setup [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/sbt-davenverse_2.12_1.0/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/sbt-davenverse_2.12_1.0) ![Code of Consuct](https://img.shields.io/badge/Code%20of%20Conduct-Scala-blue.svg)


## Quick Start

To use sbt-davenverse in an existing SBT project with Scala 2.11 or a later version, add the following dependencies to your
`plugins.sbt` depending on your needs:

```scala
addSbtPlugin("io.chrisdavenport" % "sbt-davenverse" % "<version>")
```

A basic setup for a page with a website like these other website might look like the following 


```sbt
ThisBuild / crossScalaVersions := Seq("2.12.13", "2.13.5")

lazy val `my-cool-project` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .aggregate(core)

lazy val core = project.in(file("core"))
  .settings(name := "my-cool-project")

lazy val site = project.in(file("site"))
  .enablePlugins(NoPublishPlugin)
  .enablePlugins(DavenverseSitePlugin)
  .disablePlugins(MimaPlugin)
  .dependsOn(core)
```

Meanwhile on the other side of the coin if you don't want a microsite, then you might make something more minimal, in which case you don't need to add anymore settings to get full ci-testing, and the expected configurations in place.


```sbt
name := "minimal-example"
```


The setting keys that this makes available are

- `davenverseGithubOwner` - automatically picked up from git, but can be provided
- `davenverseGithubRepoName` - automatically picked up from git, but can be provided

In the site plugin

- `davenverseSiteScalaVersion` - This is the site scala version that is used to derive the condition for the microsite. Which is the highest version of scala 2 supported ideally.
- `davenverseSiteConditional` - This is used for the site conditions, may be necessary checking to change to add features for unconsidered conditionals for this plugin yet.
