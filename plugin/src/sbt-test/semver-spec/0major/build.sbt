import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._

ThisBuild / versionScheme := Some("semver-spec")

lazy val root = (project in file(".")).settings(
  version := "0.0.2-SNAPSHOT",
  scalaVersion := "2.13.4",
  versionSchemeEnforcerPreviousVersion := Some("0.0.1"),
  TaskKey[Unit]("check") := {
    val expected: Option[Either[Throwable, VersionChangeType]] =
      Some(Right(VersionChangeType.Major))
    val actual: Option[Either[Throwable, VersionChangeType]] =
      versionSchemeEnforcerChangeType.value
    if (actual == expected) {
      ()
    } else {
      sys.error(s"Expected ${expected}, got ${actual}")
    }
  }
).enablePlugins(SbtVersionSchemeEnforcerPlugin)
