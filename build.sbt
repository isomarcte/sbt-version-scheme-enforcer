import _root_.io.isomarcte.sbt.version.scheme.enforcer.build.GAVs._
import _root_.io.isomarcte.sbt.version.scheme.enforcer.build.JREMajorVersion
import _root_.io.isomarcte.sbt.version.scheme.enforcer.build._
import java.net.URL

// Constants //

lazy val isomarcteOrg: String       = "io.isomarcte"
lazy val jreVersionForDocs: String  = JREMajorVersion.majorVersion
lazy val projectName: String        = "sbt-version-scheme-enforcer"
lazy val projectUrl: URL            = url("https://github.com/isomarcte/sbt-version-scheme-enforcer")
lazy val scala212: String           = "2.12.13"
lazy val scalaVersions: Set[String] = Set(scala212)

// SBT Command Aliases //

// Usually run before making a PR
addCommandAlias(
  "full_build",
  ";+clean;githubWorkflowGenerate;+test;+test:doc;+versionSchemeEnforcerCheck;+scalafmtAll;+scalafmtSbt;+scalafixAll;+scripted;"
)

// ThisBuild //

// General

ThisBuild / versionScheme := Some("pvp")

ThisBuild / scalacOptions ++= List("-target:jvm-1.8")

ThisBuild / organization := isomarcteOrg
ThisBuild / scalafixDependencies ++= List(organizeImportsG %% organizeImportsA % organizeImportsV)
ThisBuild / scalafixScalaBinaryVersion := scalaBinaryVersion.value
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
// We only publish on 2.12.x to keep in line with SBT, but it is assumed that
// SBT will get to 2.13.x someday, so this ensures we stay up to date.
ThisBuild / crossScalaVersions := scalaVersions.toSeq

// MUnit

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

// GithubWorkflow

ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowOSes := Set("macos-latest", "ubuntu-latest").toList
ThisBuild / githubWorkflowJavaVersions := Set("adopt@1.11", "adopt@1.16", "adopt@1.8").toList
ThisBuild / githubWorkflowBuildPreamble :=
  List(
    WorkflowStep.Sbt(List("scalafmtSbtCheck", "scalafmtCheckAll", "versionSchemeEnforcerCheck")),
    WorkflowStep.Run(List("sbt 'scalafixAll --check'")),
    WorkflowStep.Sbt(List("publishLocal")),
    WorkflowStep.Run(List("sbt scripted", "./run-vcs-tests.sh")),
    WorkflowStep.Sbt(List("doc"))
  )
ThisBuild / githubWorkflowBuildPostamble := List(WorkflowStep.Sbt(List("test:doc")))

// Doc Settings //

lazy val docSettings: List[Def.Setting[_]] = List(
  apiURL := {
    val moduleName: String = name.value
    val org: String        = organization.value
    Some(url(s"https://www.javadoc.io/doc/${org}/${moduleName}_${scalaBinaryVersion.value}/latest/index.html"))
  },
  autoAPIMappings := true,
  Compile / doc / apiMappings ++= {
    ScalaDocLinks.mappings(Seq((Compile / dependencyClasspathAsJars).value), scalaBinaryVersion.value) ++
      ScalaDocLinks.jreModuleLinks(jreVersionForDocs)
  },
  Compile / doc / scalacOptions ++= List("-no-link-warnings") // JDK module linking is broken on 2.12.12
)

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
  Test / publishArtifact := false,
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
  .disablePlugins(SbtVersionSchemeEnforcerPlugin)

// Core //

lazy val core: Project = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}-core",
    libraryDependencies ++= List(coursierG %% coursierVersionsA % coursierVersionsV) ++
      List(scalametaG %% munitA % munitV).map(_ % Test),
    docSettings
  )

// Plugin //

lazy val plugin: Project = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}-plugin",
    addSbtPlugin(typesafeG % sbtMimaPluginA % sbtMimaPluginV),
    libraryDependencies ++=
      List(
        coursierG  %% coursierVersionsA         % coursierVersionsV,
        scalaSbtG   % sbtA                      % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtCollectionsA           % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtCoreMacrosA            % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtLibraryManagementCoreA % sbtLibraryManagementCoreV % Provided,
        scalaSbtG  %% sbtMainA                  % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtMainSettingsA          % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtTaskSystemA            % sbtVersion.value          % Provided,
        scalaSbtG  %% sbtUtilPositionA          % sbtVersion.value          % Provided,
        scalametaG %% munitA                    % munitV                    % Test
      ),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .enablePlugins(SbtPlugin)
  .disablePlugins(SbtVersionSchemeEnforcerPlugin)
  .dependsOn(core)
