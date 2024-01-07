import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import sbtghactions.UseRef

val Scala212 = "2.12.14"

ThisBuild / crossScalaVersions := Seq(Scala212)
ThisBuild / scalaVersion := crossScalaVersions.value.last

ThisBuild / githubWorkflowArtifactUpload := false

val Scala212Cond = s"matrix.scala == '$Scala212'"

def rubySetupSteps(cond: Option[String]) = Seq(
  WorkflowStep.Use(
    UseRef.Public("ruby", "setup-ruby", "v1"),
    name = Some("Setup Ruby"),
    params = Map("ruby-version" -> "3.0.1"),
    cond = cond),

  WorkflowStep.Run(
    List(
      "gem install saas",
      "gem install jekyll -v 4.2.0"),
    name = Some("Install microsite dependencies"),
    cond = cond))

ThisBuild / githubWorkflowBuildPreamble ++=
  rubySetupSteps(Some(Scala212Cond))

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("test")),

  WorkflowStep.Sbt(
    List("site/makeMicrosite"),
    cond = Some(Scala212Cond)))

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")

// currently only publishing tags
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(RefPredicate.StartsWith(Ref.Tag("v")), RefPredicate.Equals(Ref.Branch("main")))

ThisBuild / githubWorkflowPublishPreamble ++=
  WorkflowStep.Use(UseRef.Public("olafurpg", "setup-gpg", "v3")) +: rubySetupSteps(None)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    name = Some("Publish artifacts to Sonatype"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}")),
  WorkflowStep.Use(UseRef.Public("christopherdavenport", "create-ghpages-ifnotexists", "v1")),
  WorkflowStep.Sbt(
    List("site/publishMicrosite"),
    name = Some("Publish microsite")
  )
)


val catsV = "2.3.1"
val catsEffectV = "2.3.1"
// val shapelessV = "2.3.3"
val fs2V = "2.5.0"
val http4sV = "0.21.15"
val circeV = "0.13.0"
val doobieV = "0.9.4"
val log4catsV = "1.1.1"

val munitCatsEffectV = "0.12.0"

val kindProjectorV = "0.13.2"
val betterMonadicForV = "0.3.1"

// Projects
lazy val `sbt-davenverse` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .aggregate(core)

lazy val core = project.in(file("core"))
  .enablePlugins(SbtPlugin)
  .settings(commonSettings)
  .settings(
    name := "sbt-davenverse",

    addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.9.2"),
    addSbtPlugin("io.chrisdavenport" % "sbt-mima-version-check" % "0.1.2"),
    addSbtPlugin("io.chrisdavenport" % "sbt-no-publish" % "0.1.0"),

    addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0"),
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.1"),

    addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7"),

    addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.12.0"),

    addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.5.2"),
    addSbtPlugin("com.47deg" % "sbt-microsites" % "1.3.4"),
    addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3"),

    scriptedBufferLog := false,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    test := {
      (Test / test).value
      scripted.toTask("").value
    }
  )

lazy val site = project.in(file("site"))
  .disablePlugins(MimaPlugin)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(MdocPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(commonSettings)
  .dependsOn(core)
  .settings{
    import microsites._
    Seq(
      micrositeName := "sbt-davenverse",
      micrositeDescription := "Shared Configs and Setup",
      micrositeAuthor := "Christopher Davenport",
      micrositeGithubOwner := "davenverse",
      micrositeGithubRepo := "sbt-davenverse",
      micrositeBaseUrl := "/sbt-davenverse",
      micrositeDocumentationUrl := "https://www.javadoc.io/doc/io.chrisdavenport/sbt-davenverse_2.12",
      micrositeGitterChannelUrl := "ChristopherDavenport/libraries", // Feel Free to Set To Something Else
      micrositeFooterText := None,
      micrositeHighlightTheme := "atom-one-light",
      micrositePalette := Map(
        "brand-primary" -> "#3e5b95",
        "brand-secondary" -> "#294066",
        "brand-tertiary" -> "#2d5799",
        "gray-dark" -> "#49494B",
        "gray" -> "#7B7B7E",
        "gray-light" -> "#E5E5E6",
        "gray-lighter" -> "#F4F3F4",
        "white-color" -> "#FFFFFF"
      ),
      micrositePushSiteWith := GitHub4s,
      micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
      micrositeExtraMdFiles := Map(
          file("CODE_OF_CONDUCT.md")  -> ExtraMdFileConfig("code-of-conduct.md",   "page", Map("title" -> "code of conduct",   "section" -> "code of conduct",   "position" -> "100")),
          file("LICENSE")             -> ExtraMdFileConfig("license.md",   "page", Map("title" -> "license",   "section" -> "license",   "position" -> "101"))
      )
    )
  }

// General Settings
lazy val commonSettings = Seq(
  testFrameworks += new TestFramework("munit.Framework"),
  libraryDependencies ++= {
    if (isDotty.value) Seq.empty
    else Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % kindProjectorV cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV),
    )
  },
  scalacOptions ++= {
    if (isDotty.value) Seq("-source:3.0-migration")
    else Seq()
  },
  Compile / doc / sources := {
    val old = (Compile / doc / sources).value
    if (isDotty.value)
      Seq()
    else
      old
  },
)

// General Settings
inThisBuild(List(
  organization := "io.chrisdavenport",
  developers := List(
    Developer("ChristopherDavenport", "Christopher Davenport", "chris@christopherdavenport.tech", url("https://github.com/ChristopherDavenport"))
  ),

  homepage := Some(url("https://github.com/davenverse/sbt-davenverse")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

  pomIncludeRepository := { _ => false},
  Compile / doc / scalacOptions ++= Seq(
      "-groups",
      "-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath,
      "-doc-source-url", "https://github.com/davenverse/sbt-davenverse/blob/v" + version.value + "€{FILE_PATH}.scala"
  )
))
