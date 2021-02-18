package io.isomarcte.sbt.version.scheme.enforcer.plugin

import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._
import coursier.version._
import scala.util.Try

private[plugin] object SbtVersionSchemeEnforcer {
  def versionChangeTypeFromSchemeAndPreviousVersion(
    scheme: Option[String],
    previousVersion: Option[String],
    nextVersion: String
  ): Option[Either[Throwable, VersionChangeType]] = {
    type ETV = Either[Throwable, VersionChangeType]
    previousVersion.map(previousVersion =>
      for {
        s <- schemeToVersionCompatibility(scheme)
        p <- versionToNumericVersion(previousVersion)
        n <- versionToNumericVersion(nextVersion)
        result <- VersionChangeType
          .fromPreviousAndNextNumericVersion(s)(p, n)
          .fold(e => Left(new RuntimeException(e)): ETV, value => Right(value): ETV)
      } yield result
    )
  }

  def schemeToVersionCompatibility(scheme: Option[String]): Either[Throwable, VersionCompatibility] =
    scheme.fold(
      Left(
        new RuntimeException(
          """versionScheme is empty, unset or set in the incorrect scope. In order to use sbt-version-scheme-enforcer-plugin, you must set versionScheme to, "pvp", "early-semver", or "semver-spec", e.g. `ThisBuild / versionScheme := Some("pvp")`."""
        )
      ): Either[Throwable, VersionCompatibility]
    )(scheme =>
      VersionCompatibility(scheme).fold(
        Left(
          new IllegalArgumentException(
            s"${scheme} is not a valid version scheme according to coursier.version.VersionCompatibility"
          )
        ): Either[Throwable, VersionCompatibility]
      )(value => Right(value))
    )

  def versionToNumericVersion(version: String): Either[Throwable, NumericVersion] =
    NumericVersion
      .fromCoursierVersion(Version(version))
      .fold(e => Left(new IllegalArgumentException(e)), value => Right(value))

  private val gitCommandWithTags: Seq[String] = Seq("git", "--no-pager", "describe", "--abbrev=0", "--tags", "@")

  private val gitCommandWithOutTags: Seq[String] = Seq(
    "git",
    "--no-pager",
    "describe",
    "--abbrev=0",
    "--tags",
    """--exclude=*-[!0-9]*""",
    "@"
  )

  private def normalizeVersion(value: String): String =
    if (value.startsWith("v")) {
      value.drop(1)
    } else {
      value
    }

  def previousTagFromGit: Either[Throwable, Option[String]] =
    Try(sys.process.Process(gitCommandWithOutTags, None).lineStream.headOption)
      .orElse(Try(sys.process.Process(gitCommandWithTags, None).lineStream.headOption))
      .toEither
      .map(_.map(normalizeVersion))
}
