package io.isomarcte.sbt.version.scheme.enforcer.plugin

import scala.collection.immutable.SortedSet
import _root_.io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._
import _root_.io.isomarcte.sbt.version.scheme.enforcer.core.project._
import _root_.io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import coursier.version.{Version => _, _}
import io.isomarcte.sbt.version.scheme.enforcer.core.project.ProjectVersionInfo
import io.isomarcte.sbt.version.scheme.enforcer.core.project.BinaryCheckInfo

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
    previousVersion
      .orElse(initialVersion)
      .fold(Right(Option.empty[NumericVersion]): Either[Throwable, Option[NumericVersion]])(value =>
        NumericVersion.fromStringT(value).map(value => Option(value))
      )
      .flatMap(previousVersion =>
        NumericVersion
          .fromStringT(nextVersion)
          .flatMap(nextVersion =>
            versionChangeTypeFromSchemeAndPreviousVersionNumeric(scheme, previousVersion, nextVersion)
          )
      )

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

  def validateVersionScheme(value: Option[String]): Either[String, VersionScheme] =
    value.fold(
      Left("versionScheme is not set, but is required to use sbt-version-scheme-enforcer-plugin."): Either[String, VersionScheme]
    )(value =>
      VersionScheme.fromCoursierVersionString(value).fold(
        Left(s"Unknown versionScheme value: ${value}"): Either[String, VersionScheme]
      )(value =>
        Right(value)
      )
    )
}
