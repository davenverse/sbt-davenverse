package sbtdavenverse

import sbt._
import Keys._
import sbtghactions.{GitHubActionsPlugin, GitHubActionsKeys, GenerativeKeys, WorkflowStep, Ref, RefPredicate}, GitHubActionsPlugin._, GitHubActionsKeys._, GenerativeKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbtcrossproject.CrossPlugin

object DavenversePlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = GitHubActionsPlugin &&
    // CrossPlugin &&
    plugins.JvmPlugin

  object autoImport {
    lazy val isDotty = settingKey[Boolean]("True if building with Scala 3")

    lazy val davenverseGithubOwner = settingKey[String]("Github User or Organization which hosts repo")
    lazy val davenverseGithubRepoName = settingKey[String]("Github Repo Name")

  }
  import autoImport._

  override def globalSettings: Seq[Setting[_]] = Seq(
    crossScalaVersions := Seq("2.12.14", "2.13.6", "3.0.0"),
    Def.derive(scalaVersion := crossScalaVersions.value.last, default = true),
    Def.derive(isDotty := scalaVersion.value.startsWith("3."))
  )

  override def projectSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= {
      if (isDotty.value)
        Nil
      else
        Seq(
          compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full),
        )
    },

    Compile / doc/ sources := {
      val old = (Compile / doc / sources).value
      if (isDotty.value)
        Seq()
      else
        old
    },
  )

  override def buildSettings: Seq[Setting[_]] = Seq(
    davenverseGithubOwner := gitRemoteInfo._1,
    davenverseGithubRepoName := gitRemoteInfo._2,
    organization := "io.chrisdavenport",
    developers := List(
      Developer("ChristopherDavenport", "Christopher Davenport", "chris@christopherdavenport.tech", url("https://github.com/ChristopherDavenport"))
    ),
    licenses ++= Seq("MIT" -> url("http://opensource.org/licenses/MIT")),

    homepage := Some(url("https://github.com/" + davenverseGithubOwner.value + "/" + davenverseGithubRepoName.value)),

    pomIncludeRepository := { _ => false},
    Compile / doc / scalacOptions ++= Seq(
        "-groups",
        "-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath,
        "-doc-source-url", "https://github.com/" + 
          davenverseGithubOwner.value + "/" + davenverseGithubRepoName.value + 
          "/blob/v" + version.value + "€{FILE_PATH}.scala"
    ),

    githubWorkflowArtifactUpload := false,
    githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "mimaReportBinaryIssues"))),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")), RefPredicate.Equals(Ref.Branch("main"))),
    githubWorkflowTargetTags ++= Seq("v*"),
    githubWorkflowPublishPreamble ++= Seq(WorkflowStep.Use("olafurpg", "setup-gpg", "v3")),
    githubWorkflowPublish ++= Seq(
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

    /** Gets the Github user and repository from the git remote info */
  private val gitRemoteInfo = {
    import scala.sys.process._

    val identifier = """([^\/]+)"""

    val GitHubHttps   = s"https://github.com/$identifier/$identifier".r
    val SSHConnection = s"git@github.com:$identifier/$identifier.git".r

    try {
      val remote = List("git", "ls-remote", "--get-url", "origin").!!.trim()

      remote match {
        case GitHubHttps(user, repo)   => (user, repo)
        case SSHConnection(user, repo) => (user, repo)
        case _                         => ("", "")
      }
    } catch {
      case scala.util.control.NonFatal(_) => ("", "")
    }
  }

}
/*
  */