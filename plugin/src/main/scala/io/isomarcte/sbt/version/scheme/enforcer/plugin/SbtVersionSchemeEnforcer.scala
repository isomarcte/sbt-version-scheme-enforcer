package io.isomarcte.sbt.version.scheme.enforcer.plugin

import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._
import cats._
import cats.effect._
import cats.syntax.all._
import coursier.version._

private[plugin] object SbtVersionSchemeEnforcer {
  def versionChangeTypeFromSchemeAndPreviousVersion[F[_]](
    scheme: Option[String],
    previousVersion: Option[String],
    nextVersion: String
  )(implicit F: Sync[F]): F[VersionChangeType] =
    previousVersion.fold(
      F.raiseError[VersionChangeType](
        new RuntimeException(
          "versionSchemeEnforcerPreviousVersion is unset and/or could not be derived from VCS, e.g. git. In order to use sbt-version-scheme-enforcer-plugin this value must be set or derivable from VCS."
        )
      )
    )(previousVersion =>
      (
        schemeToVersionCompatibility(scheme),
        versionToNumericVersion(previousVersion),
        versionToNumericVersion(nextVersion)
      ).tupled
        .flatMap { case (scheme, previousVersion, nextVersion) =>
          VersionChangeType
            .fromPreviousAndNextNumericVersion(scheme)(previousVersion, nextVersion)
            .fold(e => F.raiseError[VersionChangeType](new RuntimeException(e)), _.pure[F])
        }
    )

  def schemeToVersionCompatibility[F[_]](
    scheme: Option[String]
  )(implicit F: ApplicativeError[F, Throwable]): F[VersionCompatibility] =
    scheme.fold(
      F.raiseError[VersionCompatibility](
        new RuntimeException(
          """versionScheme is empty, unset or set in the incorrect scope. In order to use sbt-version-scheme-enforcer-plugin, you must set versionScheme to, "pvp", "early-semver", or "semver-spec", e.g. `ThisBuild / versionScheme := Some("pvp")`."""
        )
      )
    )(scheme =>
      VersionCompatibility(scheme).fold(
        F.raiseError[VersionCompatibility](
          new IllegalArgumentException(
            s"${scheme} is not a valid version scheme according to coursier.version.VersionCompatibility"
          )
        )
      )(_.pure[F])
    )

  def versionToNumericVersion[F[_]](version: String)(implicit F: ApplicativeError[F, Throwable]): F[NumericVersion] =
    F.fromEither(NumericVersion.fromCoursierVersion(Version(version)).leftMap(e => new IllegalArgumentException(e)))

  private val command: Seq[String] = Seq("git", "--no-pager", "describe", "--abbrev=0", "@")

  private def normalizeVersion(value: String): String =
    if (value.startsWith("v")) {
      value.drop(1)
    } else {
      value
    }

  def previousTagFromGit[F[_]](implicit F: Sync[F]): F[Option[String]] =
    F.delay(sys.process.Process(command, None).lineStream.headOption.map(normalizeVersion))
}
