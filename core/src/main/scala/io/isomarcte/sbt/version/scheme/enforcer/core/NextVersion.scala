package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.data._
import cats.syntax.all._
import coursier.version._

object NextVersion {

  private val zero: BigInt = BigInt(0)
  private val one: BigInt  = BigInt(1)

  def minimumNextVersion(
    versionCompatibility: VersionCompatibility
  )(versionChangeType: VersionChangeType, currentVersion: Version): Either[String, Version] =
    NumericVersion
      .fromCoursierVersion(currentVersion)
      .flatMap(numericVersion => minimumNextNumericVersion(versionCompatibility)(versionChangeType, numericVersion))
      .map(_.asVersion)

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
          NumericVersion.fromChain(NonEmptyChain.of(a, b + one, zero))
        case VersionChangeType.Minor =>
          NumericVersion.fromChain(NonEmptyChain.of(a, b, c + one))
        case VersionChangeType.Patch =>
          currentVersion
            ._4
            .fold(
              // The _very_ next version if there is no D component is A.B.C.0
              NumericVersion.fromChain(NonEmptyChain.of(currentVersion._1, b, c, zero))
            )(d => NumericVersion.fromChain(NonEmptyChain.of(currentVersion._1, b, c, d + one)))
      }
    ).getOrElse(invalidError)
  }

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
                  NumericVersion.fromChain(NonEmptyChain.of(currentVersion._1, b + one, zero))
                case VersionChangeType.Minor | VersionChangeType.Patch =>
                  NumericVersion.fromChain(NonEmptyChain.of(currentVersion._1, b, c + one))
              }
            } else {
              minimumNextSemVerSpec(versionChangeType, currentVersion)
            }
        ).getOrElse(invalidError)
      }(Function.const(invalidError))
  }

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
              NumericVersion.fromChain(NonEmptyChain.of(currentVersion._1, b, c + one))
            } else {
              versionChangeType match {
                case VersionChangeType.Major =>
                  NumericVersion.fromChain(NonEmptyChain.of(currentVersion._1 + one, zero, zero))
                case VersionChangeType.Minor =>
                  NumericVersion.fromChain(NonEmptyChain.of(currentVersion._1, b + one, zero))
                case VersionChangeType.Patch =>
                  NumericVersion.fromChain(NonEmptyChain.of(currentVersion._1, b, c + one))
              }
            }
        ).getOrElse(invalidError)
      }(Function.const(invalidError))
  }
}
