package sbtdavenverse

import sbt._
import Keys._
import sbtghactions.{GitHubActionsPlugin, GitHubActionsKeys, GenerativeKeys, WorkflowStep, Ref, RefPredicate}, GitHubActionsPlugin._, GitHubActionsKeys._, GenerativeKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbtcrossproject.CrossPlugin
import sbtghactions.UseRef

object DavenverseGithubActionsPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = GitHubActionsPlugin &&
    plugins.JvmPlugin

  override def globalSettings: Seq[Setting[_]] =  Nil

  override def projectSettings: Seq[Setting[_]] = Nil

  override def buildSettings: Seq[Setting[_]] = Seq(
    githubWorkflowArtifactUpload := false,
    githubWorkflowJavaVersions := Seq("adopt@1.8", "adopt@1.11"),
    // Settings I expect may need some sort of additional configuration in the future
    githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "mimaReportBinaryIssues"))),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")), RefPredicate.Equals(Ref.Branch("main"))),
    githubWorkflowTargetTags ++= Seq("v*"),

    githubWorkflowPublishPreamble ++= Seq(WorkflowStep.Use(UseRef.Public("olafurpg", "setup-gpg", "v3"))),
    githubWorkflowPublish := Seq(
      WorkflowStep.Sbt(
        List("ci-release"),
        name = Some("Publish artifacts to Sonatype"),
        env = Map(
          "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
          "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
          "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
          "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
        )
      )
    )
  )
}