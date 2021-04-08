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
ThisBuild / githubWorkflowOSes := Set("macos-latest", "windows-latest", "ubuntu-latest").toList
ThisBuild / githubWorkflowJavaVersions := Set("adopt@1.11", "adopt@1.16", "adopt@1.8").toList
ThisBuild / githubWorkflowBuildPreamble :=
  List(
    WorkflowStep.Sbt(List("scalafmtSbtCheck", "scalafmtCheckAll")),
    WorkflowStep.Run(List("sbt 'scalafixAll --check'")),
    WorkflowStep.Run(List("sbt versionSchemeEnforcerCheck")),
    WorkflowStep.Sbt(List("publishLocal")),
    WorkflowStep.Sbt(List("scripted")),
    WorkflowStep.Sbt(List("doc"))
  )
ThisBuild / githubWorkflowBuildPostamble := List(WorkflowStep.Sbt(List("test:doc")))
ThisBuild / githubWorkflowBuildMatrixExclusions :=
  List(
    // For some reason the `githubWorkflowCheck` step gets stuck with this
    // particular combination.
    MatrixExclude(Map("os" -> "windows-latest"))
  )
ThisBuild / githubWorkflowBuildMatrixInclusions :=
  List(
    // Give windows a chance with the latest LTS JVM.
    MatrixInclude(matching = Map("java" -> "adopt@1.11"), additions = Map("os" -> "windows-latest"))
  )

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
        coursierG %% coursierVersionsA         % coursierVersionsV,
        scalaSbtG  % sbtA                      % sbtVersion.value,
        scalaSbtG %% sbtCollectionsA           % sbtVersion.value,
        scalaSbtG %% sbtCoreMacrosA            % sbtVersion.value,
        scalaSbtG %% sbtLibraryManagementCoreA % sbtVersion.value,
        scalaSbtG %% sbtMainA                  % sbtVersion.value,
        scalaSbtG %% sbtMainSettingsA          % sbtVersion.value,
        scalaSbtG %% sbtTaskSystemA            % sbtVersion.value,
        scalaSbtG %% sbtUtilPositionA          % sbtVersion.value
      ),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++ Seq("-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .enablePlugins(SbtPlugin)
  .disablePlugins(SbtVersionSchemeEnforcerPlugin)
  .dependsOn(core)
