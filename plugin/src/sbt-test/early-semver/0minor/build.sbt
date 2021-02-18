import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._

ThisBuild / versionScheme := Some("early-semver")

lazy val root = (project in file(".")).settings(
  version := "0.1.1-SNAPSHOT",
  scalaVersion := "2.13.4",
  versionSchemeEnforcerPreviousVersion := Some("0.1.0"),
  TaskKey[Unit]("check") := {
    val expected: Option[Either[Throwable, VersionChangeType]] =
      Some(Right(VersionChangeType.Minor))
    val actual: Option[Either[Throwable, VersionChangeType]] =
      versionSchemeEnforcerChangeType.value
    if (actual == expected) {
      ()
    } else {
      sys.error(s"Expected ${expected}, got ${actual}")
    }
  }
).enablePlugins(SbtVersionSchemeEnforcerPlugin)
