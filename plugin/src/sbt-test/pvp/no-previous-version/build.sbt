import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._

ThisBuild / versionScheme := Some("pvp")

lazy val root = (project in file(".")).settings(
  version := "0.1.0.0-SNAPSHOT",
  scalaVersion := "2.13.4",
  versionSchemeEnforcerInitialVersion := Some("0.1.0.0"),
  TaskKey[Unit]("check") := {
    val expected: Set[ModuleID] =
      Set.empty
    val actual: Set[ModuleID] =
      mimaPreviousArtifacts.value
    if (actual == expected) {
      ()
    } else {
      sys.error(s"Expected ${expected}, got ${actual}")
    }
  }
).enablePlugins(SbtVersionSchemeEnforcerPlugin)
