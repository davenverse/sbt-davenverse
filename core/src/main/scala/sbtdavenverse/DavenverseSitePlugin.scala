package sbtdavenverse

import sbt._
import Keys._
import sbtghactions.{GitHubActionsPlugin, GitHubActionsKeys, GenerativeKeys, WorkflowStep}, GitHubActionsPlugin._, GitHubActionsKeys._, GenerativeKeys._


object DavenverseSitePlugin extends AutoPlugin {

  override def requires = DavenversePlugin && plugins.JvmPlugin

  object autoImport {
    val davenverseSiteScalaVersion = settingKey[String]("Site Scala Release Version")
    val davenverseSiteConditional = settingKey[String]("Site Conditional When To Add Site Steps")
  }
  import autoImport._

  override def globalSettings: Seq[Setting[_]] = Seq()

  

  override def projectSettings: Seq[Setting[_]] = Nil

  override def buildSettings: Seq[Setting[_]] = Seq(
    davenverseSiteScalaVersion := crossScalaVersions.value.toList.reverse.collect{
      case x if x.startsWith("2.13") => x
      case x if x.startsWith("2.12") => x
    }.headOption.getOrElse(scalaVersion.value), // What to do here if we don't have a valid scala 2 version
    davenverseSiteConditional := "matrix.scala == '" ++ davenverseSiteScalaVersion.value ++ "'",
    githubWorkflowBuildPreamble ++= rubySetupSteps(Some(davenverseSiteConditional.value)),
    githubWorkflowPublishPreamble ++= rubySetupSteps(None),
    githubWorkflowPublish ++= Seq(
      WorkflowStep.Use("christopherdavenport", "create-ghpages-ifnotexists", "v1"),
      WorkflowStep.Sbt(
        List("site/publishMicrosite"),
        name = Some("Publish microsite")
      )
    )
  )

  def rubySetupSteps(cond: Option[String]) = Seq(
    WorkflowStep.Use(
      "ruby", "setup-ruby", "v1",
      name = Some("Setup Ruby"),
      params = Map("ruby-version" -> "2.6.0"),
      cond = cond),

    WorkflowStep.Run(
      List(
        "gem install saas",
        "gem install jekyll -v 3.2.1"),
      name = Some("Install microsite dependencies"),
      cond = cond))
}