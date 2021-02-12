package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

sealed trait VersionChangeType extends Product with Serializable

object VersionChangeType {
  case object Major extends VersionChangeType
  case object Minor extends VersionChangeType
  case object Patch extends VersionChangeType

  private[this] def pad(a: NumericVersion, b: NumericVersion): List[(Option[BigInt], Option[BigInt])] = {
    val len: Int = scala.math.max(a.asVector.size, b.asVector.size).toInt

    (
      a.asVector
        .toList
        .map(value => Some(value))
        .padTo(len, None)
        .zip(b.asVector.toList.map(value => Some(value)).padTo(len, None))
    )
  }

  def fromPreviousAndNextVersion(
    versionCompatibility: VersionCompatibility
  )(previousVersion: Version, nextVersion: Version): Either[String, VersionChangeType] =
    for {
      p      <- NumericVersion.fromCoursierVersion(previousVersion)
      n      <- NumericVersion.fromCoursierVersion(nextVersion)
      result <- fromPreviousAndNextNumericVersion(versionCompatibility)(p, n)
    } yield result

  def fromPreviousAndNextNumericVersion(
    versionCompatibility: VersionCompatibility
  )(previousVersion: NumericVersion, nextVersion: NumericVersion): Either[String, VersionChangeType] = {
    lazy val padded: List[(Option[BigInt], Option[BigInt])] = pad(previousVersion, nextVersion)
    versionCompatibility match {
      case VersionCompatibility.Default | VersionCompatibility.PackVer =>
        padded match {
          case (Some(prevA), Some(nextA)) :: (Some(_), Some(_)) :: (Some(_), Some(_)) :: _ if prevA < nextA =>
            Right(Major)
          case (Some(prevA), Some(nextA)) :: (Some(prevB), Some(nextB)) :: (Some(_), Some(_)) :: _
              if prevA === nextA && prevB < nextB =>
            Right(Major)
          case (Some(prevA), Some(nextA)) :: (Some(prevB), Some(nextB)) :: (Some(prevC), Some(nextC)) :: _
              if prevA === nextA && prevB === nextB && prevC < nextC =>
            Right(Minor)
          case (Some(prevA), Some(nextA)) ::
              (Some(prevB), Some(nextB)) ::
              (Some(prevC), Some(nextC)) ::
              (Some(prevD), Some(nextD)) :: _
              if prevA === nextA && prevB === nextB && prevC === nextC && prevD < nextD =>
            Right(Patch)
          case (Some(prevA), Some(nextA)) ::
              (Some(prevB), Some(nextB)) ::
              (Some(prevC), Some(nextC)) ::
              (Some(prevD), Some(nextD)) :: Nil
              if prevA === nextA && prevB === nextB && prevC === nextC && prevD === nextD =>
            // Version might be the same after tag removal, in this case no binary constraints change
            Right(Patch)
          case (Some(prevA), Some(nextA)) :: (Some(prevB), Some(nextB)) :: (Some(prevC), Some(nextC)) :: Nil
              if prevA === nextA && prevB === nextB && prevC === nextC =>
            // Version might be the same after tag removal, in this case no binary constraints change
            Right(Patch)
          case (Some(prevA), Some(nextA)) ::
              (Some(prevB), Some(nextB)) ::
              (Some(prevC), Some(nextC)) ::
              (None, Some(_)) :: _ if prevA === nextA && prevB === nextB && prevC === nextC =>
            Right(Patch)
          case _ =>
            Left(
              s"Invalid versions for PVP. Previous must be < Next and each version should have at least three components. Previous: ${previousVersion
                .versionString}, Next: ${nextVersion.versionString}"
            ): Either[String, VersionChangeType]
        }
      case VersionCompatibility.SemVerSpec =>
        padded match {
          // Any change for SemVerSpec if < 1.0.0 should be considered binary breaking.
          case (Some(prevA), Some(nextA)) :: (Some(_), Some(_)) :: (Some(_), Some(_)) :: Nil
              if prevA === nextA && nextA === BigInt(0) =>
            Right(Major)
          case (Some(prevA), Some(nextA)) :: (Some(_), Some(_)) :: (Some(_), Some(_)) :: Nil if prevA < nextA =>
            Right(Major)
          case (Some(prevA), Some(nextA)) :: (Some(prevB), Some(nextB)) :: (Some(_), Some(_)) :: Nil
              if prevA === nextA && prevB < nextB =>
            Right(Minor)
          case (Some(prevA), Some(nextA)) :: (Some(prevB), Some(nextB)) :: (Some(prevC), Some(nextC)) :: Nil
              if prevA === nextA && prevB === nextB && prevC <= nextC =>
            // Version might be the same after tag removal, hence prevC <= nextC instead of prevC < nextC, in this case no binary constraints change
            Right(Patch)
          case _ =>
            Left(
              s"Invalid versions for SemVerSpec. Previous must be < Next and each version must have exactly three components. Previous: ${previousVersion
                .versionString}, Next: ${nextVersion.versionString}"
            ): Either[String, VersionChangeType]
        }
      case VersionCompatibility.EarlySemVer =>
        lazy val invalid: Either[String, VersionChangeType] =
          Left(
            s"Invalid versions for EarlySemVer. Previous must be < Next and each version must have exactly three components. Previous: ${previousVersion
              .versionString}, Next: ${nextVersion.versionString}"
          ): Either[String, VersionChangeType]
        padded match {
          case (Some(prevA), Some(nextA)) :: (Some(prevB), Some(nextB)) :: (Some(prevC), Some(nextC)) :: Nil
              if prevA === nextA && nextA === BigInt(0) =>
            // < 1.0.0
            if (prevB < nextB) {
              Right(Major)
            } else if (prevC < nextC) {
              Right(Minor)
            } else {
              invalid
            }
          case (Some(prevA), Some(nextA)) :: (Some(_), Some(_)) :: (Some(_), Some(_)) :: Nil if prevA < nextA =>
            Right(Major)
          case (Some(prevA), Some(nextA)) :: (Some(prevB), Some(nextB)) :: (Some(_), Some(_)) :: Nil
              if prevA === nextA && prevB < nextB =>
            Right(Minor)
          case (Some(prevA), Some(nextA)) :: (Some(prevB), Some(nextB)) :: (Some(prevC), Some(nextC)) :: Nil
              if prevA === nextA && prevB === nextB && prevC <= nextC =>
            // Version might be the same after tag removal, hence prevC <= nextC instead of prevC < nextC, in this case no binary constraints change
            Right(Patch)
          case _ =>
            invalid
        }
      case otherwise =>
        Left(
          s"Calculating the intended binary compatibility change is not supported for version numbers with version scheme: ${otherwise}"
        ): Either[String, VersionChangeType]
    }
  }
}
