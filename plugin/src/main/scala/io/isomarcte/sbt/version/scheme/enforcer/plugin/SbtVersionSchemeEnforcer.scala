package io.isomarcte.sbt.version.scheme.enforcer.plugin

import _root_.io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._

private[plugin] object SbtVersionSchemeEnforcer {
  type ETV = Either[Throwable, VersionChangeType]

  /** Attempt to derive the [[VersionChangeType]] from the scheme, previous, and
    * next version values.
    *
    * This functions input parameters are tuned for taking in values from the
    * various SBT related settings.
    */
  def versionChangeTypeFromSchemeAndPreviousVersion(
    scheme: Option[String],
    initialVersion: Option[String],
    previousVersion: Option[String],
    nextVersion: String
  ): Either[Throwable, VersionChangeType] =
    previousVersion.orElse(initialVersion).fold(
      Left(new IllegalArgumentException("Neither the previous version or the initial version were defined. At least one must be defined.")): Either[Throwable, VersionChangeType]
    )(previousVersion =>
      scheme.fold(
        Left(new IllegalArgumentException("versionScheme is not defined.")): Either[Throwable, VersionChangeType]
      )(scheme =>
        VersionScheme.fromCoursierVersionString(scheme).fold(
          Left(new IllegalArgumentException(s"Unknown versionScheme: ${scheme}")): Either[Throwable, VersionChangeType]
        )(scheme =>
          versionChangeTypeFromSchemeAndPreviousVersion(
            scheme,
            Version(previousVersion),
            Version(nextVersion)
          ).fold(
            e => Left(new IllegalArgumentException(e)): Either[Throwable, VersionChangeType],
            value => Right(value)
          )
        )
      )
    )

  def versionChangeTypeFromSchemeAndPreviousVersion(
    scheme: VersionScheme,
    initialVersion: Option[Version],
    previousVersion: Option[Version],
    nextVersion: Version
  ): Either[String, VersionChangeType] =
    scheme match {
      case VersionScheme.PVP =>
        for {
          a <- PVPVersion.fromVersion(previousVersion)
          b <- PVPVersion.fromVersion(nextVersion)
        } yield VersionChangeTypeClass[PVPVersion].changeType(a, b)
      case VersionScheme.EarlySemVer =>
        for {
          a <- EarlySemVerVersion.fromVersion(previousVersion)
          b <- EarlySemVerVersion.fromVersion(nextVersion)
        } yield VersionChangeTypeClass[EarlySemVerVersion].changeType(a, b)
      case VersionScheme.SemVer =>
        for {
          a <- SemVerVersion.fromVersion(previousVersion)
          b <- SemVerVersion.fromVersion(nextVersion)
        } yield VersionChangeTypeClass[SemVerVersion].changeType(a, b)
      case VersionScheme.Always =>
        Right(VersionChangeTypeClass[AlwaysVersion].changeType(AlwaysVersion(previousVersion.value), AlwaysVersion(nextVersion.value)))
      case VersionScheme.Strict =>
        Right(VersionChangeTypeClass[StrictVersion].changeType(StrictVersion(previousVersion.value), StrictVersion(nextVersion.value)))
    }

  /** As [[#versionChangeTypeFromSchemeAndPreviousVersion]], but the input
    * parameters are already in the proper format to perform the operation.
    */
  def versionChangeTypeFromSchemeAndPreviousVersionNumeric(
    scheme: Option[String],
    previousVersion: Option[NumericVersion],
    nextVersion: NumericVersion
  ): Either[Throwable, VersionChangeType] = {
    previousVersion
      .fold(Left(new RuntimeException("versionSchemeEnforcerPreviousVersion is unset")): ETV)(previousVersion =>
        for {
          s <- schemeToVersionCompatibility(scheme)
          result <- VersionChangeType
            .fromPreviousAndNextNumericVersion(s)(previousVersion, nextVersion)
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

  /** Checks if we should run mima by inspecting the initial version value on
    * which to enforce the given `versionScheme` and the current version.
    *
    * Tags, e.g. `-SNAPSHOT`, are removed from the current version. If they
    * weren't then the calculation would be wrong in certain circumstances.
    */
  def isAfterInitial(initialVersion: Option[String], currentVersion: String, scheme: Option[String]): Boolean =
    scheme
      .flatMap(VersionCompatibility.apply _)
      .fold(false)(scheme =>
        initialVersion.fold(true)(initialVersion =>
          SchemedVersion
            .fromVersionStringAndScheme(initialVersion, scheme)
            // Need to remove any tag, e.g. -SNAPSHOT or the comparison will be wonky.
            .compareTo(SchemedVersion.fromVersionStringAndScheme(currentVersion.takeWhile(_ =!= '-'), scheme)) < 0
        )
      )
}
