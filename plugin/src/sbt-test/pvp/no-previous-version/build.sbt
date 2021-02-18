import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._

ThisBuild / versionScheme := Some("pvp")

lazy val root = (project in file(".")).settings(
  version := "0.1.0.0-SNAPSHOT",
  scalaVersion := "2.13.4",
  versionSchemeEnforcerPreviousVersion := None,
  TaskKey[Unit]("check") := {
    val expected: Option[Either[Throwable, VersionChangeType]] =
      None
    val actual: Option[Either[Throwable, VersionChangeType]] =
      versionSchemeEnforcerChangeType.value
    if (actual == expected) {
      ()
    } else {
      sys.error(s"Expected ${expected}, got ${actual}")
    }
  }
).enablePlugins(SbtVersionSchemeEnforcerPlugin)
