import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._

ThisBuild / versionScheme := Some("pvp")
ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).settings(
  TaskKey[Unit]("check") := {()}
).enablePlugins(SbtVersionSchemeEnforcerPlugin).aggregate(both, initial, intial)

lazy val both = project.settings(
  versionSchemeEnforcerInitialVersion := Some("0.2.0.0"),
  versionSchemeEnforcerIntialVersion := Some("0.1.0.0"),
  TaskKey[Unit]("check") := {
    val initial: Option[String] = versionSchemeEnforcerInitialVersion.value
    val intial: Option[String] = versionSchemeEnforcerIntialVersion.value
    if (intial == Some("0.1.0.0") && initial == Some("0.2.0.0")) {
      println(s"[both] Initial: ${initial}")
      println(s"[both] Intial: ${intial}")
    } else {
      sys.error(s"versionSchemeEnforcerInitialVersion and versionSchemeEnforcerIntialVersion are not correct: Initial = ${initial}, Intial = ${intial}")
    }
  }
)

lazy val initial = project.settings(
  versionSchemeEnforcerInitialVersion := Some("0.2.0.0"),
  TaskKey[Unit]("check") := {
    val initial: Option[String] = versionSchemeEnforcerInitialVersion.value
    val intial: Option[String] = versionSchemeEnforcerIntialVersion.value
    if (intial == None && initial == Some("0.2.0.0")) {
      println(s"[initial] Initial: ${initial}")
      println(s"[initial] Intial: ${intial}")
    } else {
      sys.error(s"versionSchemeEnforcerInitialVersion and versionSchemeEnforcerIntialVersion are not correct: Initial = ${initial}, Intial = ${intial}")
    }
  }
)

lazy val intial = project.settings(
  versionSchemeEnforcerIntialVersion := Some("0.1.0.0"),
  TaskKey[Unit]("check") := {
    val initial: Option[String] = versionSchemeEnforcerInitialVersion.value
    val intial: Option[String] = versionSchemeEnforcerIntialVersion.value
    if (initial == intial && initial == Some("0.1.0.0")) {
      println(s"[intial] Initial: ${initial}")
      println(s"[intial] Intial: ${intial}")
    } else {
      sys.error(s"versionSchemeEnforcerInitialVersion and versionSchemeEnforcerIntialVersion are not equal: Initial = ${initial}, Intial = ${intial}")
    }
  }
)
