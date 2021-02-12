import java.net.URL

// Constants //

lazy val isomarcteOrg: String       = "io.isomarcte"
lazy val projectName: String        = "sbt-version-scheme-enforcer"
lazy val projectUrl: URL            = url("https://github.com/isomarcte/sbt-version-scheme-enforcer")
lazy val scala212: String           = "2.12.12"
lazy val scalaVersions: Set[String] = Set(scala212)

// Groups //

lazy val betterMonadicForG: String = "com.olegpy"
lazy val coursierG: String         = "io.get-coursier"
lazy val organizeImportsG          = "com.github.liancheng"
lazy val typesafeG: String         = "com.typesafe"

// Artifacts //

lazy val betterMonadicForA: String = "better-monadic-for"
lazy val coursierVersionsA: String = "versions"
lazy val kindProjectorA: String    = "kind-projector"
lazy val organizeImportsA          = "organize-imports"
lazy val sbtMimaPluginA: String    = "sbt-mima-plugin"

// Versions //

lazy val betterMonadicForV: String = "0.3.1"
lazy val coursierVersionsV: String = "0.3.0"
lazy val kindProjectorV: String    = "0.11.2"
lazy val organizeImportsV          = "0.4.4"
lazy val sbtMimaPluginV: String    = "0.8.1"

// ThisBuild //

// General

ThisBuild / organization := isomarcteOrg
ThisBuild / scalafixDependencies ++= List(organizeImportsG %% organizeImportsA % organizeImportsV)
ThisBuild / scalafixScalaBinaryVersion := scalaBinaryVersion.value
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
// We only publish on 2.12.x to keep in line with SBT, but it is assumed that
// SBT will get to 2.13.x someday, so this ensures we stay up to date.
ThisBuild / crossScalaVersions := scalaVersions.toSeq

// GithubWorkflow

ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowOSes := Set("macos-latest", "windows-latest", "ubuntu-latest").toList
ThisBuild / githubWorkflowJavaVersions := Set("adopt@1.15", "adopt@1.11", "adopt@1.8").toList
ThisBuild / githubWorkflowBuildPreamble :=
  List(
    WorkflowStep.Sbt(List("scalafmtSbtCheck", "scalafmtCheckAll")),
    WorkflowStep.Run(List("sbt 'scalafixAll --check'")),
    WorkflowStep.Sbt(List("doc"))
  )
ThisBuild / githubWorkflowBuildPostamble := List(WorkflowStep.Sbt(List("test:doc")))

// Common Settings //

lazy val commonSettings: List[Def.Setting[_]] = List(
  scalaVersion := scala212,
  addCompilerPlugin(betterMonadicForG %% betterMonadicForA % betterMonadicForV),
  addCompilerPlugin(typelevelG         % kindProjectorA    % kindProjectorV cross CrossVersion.full)
)

// Publish Settings //

lazy val publishSettings = List(
  homepage := Some(projectUrl),
  licenses := Seq("BSD3" -> url("https://opensource.org/licenses/BSD-3-Clause")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus: String = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(ScmInfo(projectUrl, s"scm:git:git@github.com:isomarcte/${projectName}.git")),
  developers :=
    List(Developer("isomarcte", "David Strawn", "isomarcte@gmail.com", url("https://github.com/isomarcte"))),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)

// Root //

lazy val root: Project = (project in file("."))
  .settings(commonSettings, publishSettings)
  .settings(
    List(
      name := projectName,
      Compile / packageBin / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false
    )
  )
  .settings(inThisBuild(commonSettings))
  .aggregate(core, plugin)

// Core //

lazy val core: Project = project.settings(
  name := s"${projectName}-core",
  libraryDependencies ++= List(coursierG %% coursierVersionsA % coursierVersionsV)
)

// Plugin //

lazy val plugin: Project = project
  .settings(name := s"${projectName}-plugin", addSbtPlugin(typesafeG % sbtMimaPluginA % sbtMimaPluginV))
  .enablePlugins(SbtPlugin)
  .dependsOn(core)
