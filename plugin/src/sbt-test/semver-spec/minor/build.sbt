import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._

ThisBuild / versionScheme := Some("semver-spec")

lazy val root = (project in file(".")).settings(
  version := "1.1.0-SNAPSHOT",
  scalaVersion := "2.13.4",
  versionSchemeEnforcerPreviousVersion := Some("1.0.0"),
  TaskKey[Unit]("check") := {
    val expected: Either[Throwable, VersionChangeType] =
      Right(VersionChangeType.Minor)
    val actual: Either[Throwable, VersionChangeType] =
      versionSchemeEnforcerChangeType.value
    if (actual == expected) {
      ()
    } else {
      sys.error(s"Expected ${expected}, got ${actual}")
    }
  }
).enablePlugins(SbtVersionSchemeEnforcerPlugin)
