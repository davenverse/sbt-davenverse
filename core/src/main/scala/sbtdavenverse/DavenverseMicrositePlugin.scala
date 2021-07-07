package sbtdavenverse

import sbt._
import Keys._
import sbtghactions.{GitHubActionsPlugin, GitHubActionsKeys, GenerativeKeys, WorkflowStep, UseRef}, GitHubActionsPlugin._, GitHubActionsKeys._, GenerativeKeys._
import _root_.microsites.MicrositesPlugin
import _root_.io.chrisdavenport.sbt.nopublish.NoPublishPlugin

object DavenverseMicrositePlugin extends AutoPlugin {

  override def requires = MicrositesPlugin && DavenversePlugin && NoPublishPlugin

  object autoImport {
    val davenverseSiteScalaVersion = settingKey[String]("Site Scala Release Version")
    val davenverseSiteConditional = settingKey[String]("Site Conditional When To Add Site Steps")
  }
  import autoImport._
  import DavenversePlugin.autoImport._

  override def globalSettings: Seq[Setting[_]] = Seq()

  override def projectSettings: Seq[Setting[_]] = {
    import microsites.MicrositeKeys._
    import microsites._
    Seq(
      micrositeName := davenverseGithubRepoName.value,
      micrositeAuthor := davenverseGithubOwner.value,
      micrositeGithubOwner := davenverseGithubOwner.value,
      micrositeGithubRepo := davenverseGithubRepoName.value,
      micrositeBaseUrl := davenverseGithubRepoName.value,
      micrositeDocumentationUrl := "https://www.javadoc.io/doc/" +
        organization.value + "/" + davenverseGithubRepoName.value + "_2.13",
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
      ),
      scalacOptions --= Seq(
        "-Xfatal-warnings",
        "-Ywarn-unused-import",
        "-Ywarn-numeric-widen",
        "-Ywarn-dead-code",
        "-Ywarn-unused:imports",
        "-Xlint:-missing-interpolator,_"
      ),
      includeFilter := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "*.svg",
    )
  }

  override def buildSettings: Seq[Setting[_]] = Seq(
    // Picks the closest scala 2 version towards the end of list of versions
    davenverseSiteScalaVersion := crossScalaVersions.value.toList.reverse.collect{
      case x if x.startsWith("2.13") => x
      case x if x.startsWith("2.12") => x
    }.headOption.getOrElse(scalaVersion.value), // What to do here if we don't have a valid scala 2 version
    // default conditional is that its on the provided scala version
    davenverseSiteConditional := "matrix.scala == '" ++ davenverseSiteScalaVersion.value ++ "'",
    // If site, then add ruby
    githubWorkflowBuildPreamble ++= rubySetupSteps(Some(davenverseSiteConditional.value)),
    githubWorkflowPublishPreamble ++= rubySetupSteps(None),
    // On publish, create the site - site is expected to always be named site
    githubWorkflowPublish ++= Seq(
      WorkflowStep.Use(UseRef.Public("christopherdavenport", "create-ghpages-ifnotexists", "v1")),
      WorkflowStep.Sbt(
        List("site/publishMicrosite"),
        name = Some("Publish microsite")
      )
    ),
    githubWorkflowBuild ++= Seq(WorkflowStep.Sbt(
    List("site/makeMicrosite"),
    cond = Some(davenverseSiteConditional.value)))
  )

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
}