package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import coursier.version.{Version => CVersion}
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** Functions for calculating the minimum next version for a given version scheme.
  *
  * @define errors Errors may occur if the given [[coursier.version.Version]]
  *         is not a valid [[NumericVersion]], if the
  *         [[coursier.version.VersionCompatibility]] is not one of the
  *         supported values, or if the [[coursier.version.Version]] value is
  *         invalid according to the version scheme as described by the
  *         [[coursier.version.VersionCompatibility]],
  *         e.g. [[coursier.version.VersionCompatibility.SemVerSpec]] versions
  *         must always have three numeric components so "1.2.3.4" would be
  *         invalid.
  */
object NextVersion {

  private val zero: BigInt = BigInt(0)
  private val one: BigInt  = BigInt(1)

  /** Calculate the minimum next version from a given
    * [[coursier.version.VersionCompatibility]] describing a version scheme, a
    * [[VersionChangeType]], and a [[coursier.version.Version]].
    *
    * $errors
    */
  def minimumNextVersion(
    versionCompatibility: VersionCompatibility
  )(versionChangeType: VersionChangeType, currentVersion: CVersion): Either[String, CVersion] =
    NumericVersion
      .fromCoursierVersion(currentVersion)
      .flatMap(numericVersion => minimumNextNumericVersion(versionCompatibility)(versionChangeType, numericVersion))
      .map(_.asVersion)

  /** As [[#minimumNextVersion]], but takes a [[java.lang.String]] rather than a
    * [[coursier.version.Version]] for convenience.
    */
  def minimumNextVersionFromString(
    versionCompatibility: VersionCompatibility
  )(versionChangeType: VersionChangeType, currentVersion: String): Either[String, CVersion] =
    minimumNextVersion(versionCompatibility)(versionChangeType, CVersion(currentVersion))

  /** As [[#minimumNextVersion]], but takes a [[NumericVersion]] directly. This
    * is sometimes more convenient if the [[coursier.version.Version]] has
    * already been parsed into a [[NumericVersion]].
    */
  def minimumNextNumericVersion(
    versionCompatibility: VersionCompatibility
  )(versionChangeType: VersionChangeType, currentVersion: NumericVersion): Either[String, NumericVersion] = {
    versionCompatibility match {
      case VersionCompatibility.Default | VersionCompatibility.PackVer =>
        minimumNextPVP(versionChangeType, currentVersion)
      case VersionCompatibility.EarlySemVer =>
        minimumNextEarlySemVer(versionChangeType, currentVersion)
      case VersionCompatibility.SemVerSpec =>
        minimumNextSemVerSpec(versionChangeType, currentVersion)
      case otherwise =>
        Left(s"""VersionCompatibility "${otherwise}" is not supported.""")
    }
  }

  /** Calculates the minimum next PVP version given the [[VersionChangeType]]
    * and last released version.
    *
    * This is equivalent to
    * `minimumNextNumericVersion(VersionCompatibility.PackVer)(versionChangeType,
    * currentVersion)`.
    *
    * @see [[https://pvp.haskell.org/ Package version Policy]]
    */
  def minimumNextPVP(
    versionChangeType: VersionChangeType,
    currentVersion: NumericVersion
  ): Either[String, NumericVersion] = {
    lazy val invalidError: Either[String, NumericVersion] = Left(
      s"PVP versions should have at least three version components"
    )
    val a: BigInt = currentVersion._1
    (
      for {
        b <- currentVersion._2
        c <- currentVersion._3
      } yield versionChangeType match {
        case VersionChangeType.Major =>
          NumericVersion.fromVector(Vector(a, b + one, zero))
        case VersionChangeType.Minor =>
          NumericVersion.fromVector(Vector(a, b, c + one))
        case VersionChangeType.Patch =>
          currentVersion
            ._4
            .fold(
              // The _very_ next version if there is no D component is A.B.C.0
              NumericVersion.fromVector(Vector(currentVersion._1, b, c, zero))
            )(d => NumericVersion.fromVector(Vector(currentVersion._1, b, c, d + one)))
      }
    ).getOrElse(invalidError)
  }

  /** Calculates the minimum next Early SemVer version given the [[VersionChangeType]]
    * and last released version.
    *
    * This is equivalent to
    * `minimumNextNumericVersion(VersionCompatibility.EarlySemVer)(versionChangeType,
    * currentVersion)`.
    *
    * @see [[https://scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html Early SemVer]]
    */
  def minimumNextEarlySemVer(
    versionChangeType: VersionChangeType,
    currentVersion: NumericVersion
  ): Either[String, NumericVersion] = {
    lazy val invalidError: Either[String, NumericVersion] = Left(
      s"EarlySemVer versions must have exactly three components, e.g. 0.1.2: ${currentVersion.asVersion}"
    )
    currentVersion
      ._4
      .fold {
        (
          for {
            b <- currentVersion._2
            c <- currentVersion._3
          } yield
            if (currentVersion._1 === zero) {
              versionChangeType match {
                case VersionChangeType.Major =>
                  NumericVersion.fromVector(Vector(currentVersion._1, b + one, zero))
                case VersionChangeType.Minor | VersionChangeType.Patch =>
                  NumericVersion.fromVector(Vector(currentVersion._1, b, c + one))
              }
            } else {
              minimumNextSemVerSpec(versionChangeType, currentVersion)
            }
        ).getOrElse(invalidError)
      }(Function.const(invalidError))
  }

  /** Calculates the minimum next SemVer version given the [[VersionChangeType]]
    * and last released version.
    *
    * This is equivalent to
    * `minimumNextNumericVersion(VersionCompatibility.SemVerSpec)(versionChangeType,
    * currentVersion)`.
    *
    * @see [[https://semver.org/ SemVer]]
    */
  def minimumNextSemVerSpec(
    versionChangeType: VersionChangeType,
    currentVersion: NumericVersion
  ): Either[String, NumericVersion] = {
    lazy val invalidError: Either[String, NumericVersion] = Left(
      s"SemVerSpec versions must have exactly three components, e.g. 0.1.2: ${currentVersion.asVersion}"
    )
    currentVersion
      ._4
      .fold {
        (
          for {
            b <- currentVersion._2
            c <- currentVersion._3
          } yield
            if (currentVersion._1 === zero) {
              NumericVersion.fromVector(Vector(currentVersion._1, b, c + one))
            } else {
              versionChangeType match {
                case VersionChangeType.Major =>
                  NumericVersion.fromVector(Vector(currentVersion._1 + one, zero, zero))
                case VersionChangeType.Minor =>
                  NumericVersion.fromVector(Vector(currentVersion._1, b + one, zero))
                case VersionChangeType.Patch =>
                  NumericVersion.fromVector(Vector(currentVersion._1, b, c + one))
              }
            }
        ).getOrElse(invalidError)
      }(Function.const(invalidError))
  }
}
