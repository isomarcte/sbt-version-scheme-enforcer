package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** Algebraic Data Type (ADT) for describing a version change with respect to
  * binary compatibility and a version scheme, e.g. "pvp", "early-semver".
  */
sealed trait VersionChangeType extends Product with Serializable

object VersionChangeType {

  /** A "Major" version change. In general this can take on very slightly
    * different meanings depending on the version scheme in use. In SemVer
    * (and Early SemVer) this strictly means a binary breaking version. In
    * PVP, this may mean a binary breaking version or it could also mean the
    * deprecation of symbols (because this can cause compilation failures if
    * warnings are fatal).
    *
    * For the purposes of the sbt-version-scheme-enforcer-plugin we are only
    * concerned about binary compatibility, as that is all that we can
    * validate at this time.
    */
  case object Major extends VersionChangeType

  /** A "Minor" version change. For SemVer, Early SemVer, and PVP, this
    * indicates the addition of new symbols in a binary compatible manner.
    *
    * A minor change ''may'' introduce source incompatibilities if the new
    * symbols conflict with other existing symbols in some specific context,
    * though this situation is relatively rare.
    */
  case object Minor extends VersionChangeType

  /** A "Patch" version change. For SemVer, Early SemVer, and PVP, this
    * indicates no changes to the visible binary API, breaking or otherwise.
    */
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

  /** Given a specific version scheme described by
    * [[coursier.version.VersionCompatibility]] and two version values,
    * calculate the type of binary version change that the change in version
    * numbers implies.
    *
    * For example,
    * For Early SemVer
    * {{{
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("1.0.0"), Version("2.0.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("1.0.0"), Version("2.0.0"))
    * res1: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Major)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("1.0.0"), Version("1.1.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("1.0.0"), Version("1.1.0"))
    * res2: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Minor)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("1.0.0"), Version("1.0.1"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("1.0.0"), Version("1.0.1"))
    * res3: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Patch)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("0.0.1"), Version("0.0.2"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("0.0.1"), Version("0.0.2"))
    * res4: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Minor)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("0.0.1"), Version("0.1.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("0.0.1"), Version("0.1.0"))
    * res5: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Major)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("0.0.1"), Version("1.0.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)(Version("0.0.1"), Version("1.0.0"))
    * res6: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Major)
    * }}}
    *
    * For SemVer
    * {{{
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)(Version("0.0.1"), Version("0.0.2"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)(Version("0.0.1"), Version("0.0.2"))
    * res7: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Major)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)(Version("1.0.0"), Version("1.0.1"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)(Version("1.0.0"), Version("1.0.1"))
    * res8: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Patch)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)(Version("1.0.0"), Version("1.1.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)(Version("1.0.0"), Version("1.1.0"))
    * res9: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Minor)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)(Version("1.0.0"), Version("2.0.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)(Version("1.0.0"), Version("2.0.0"))
    * res10: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Major)
    * }}}
    *
    * For PVP,
    * {{{
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.PackVer)(Version("0.0.0.0"), Version("0.0.0.1"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.PackVer)(Version("0.0.0.0"), Version("0.0.0.1"))
    * res11: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Patch)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.PackVer)(Version("0.0.0.0"), Version("0.0.1.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.PackVer)(Version("0.0.0.0"), Version("0.0.1.0"))
    * res12: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Minor)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.PackVer)(Version("0.0.0.0"), Version("0.1.0.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.PackVer)(Version("0.0.0.0"), Version("0.1.0.0"))
    * res13: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Major)
    *
    * scala> VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.PackVer)(Version("0.0.0.0"), Version("1.0.0.0"))
    * VersionChangeType.fromPreviousAndNextVersion(VersionCompatibility.PackVer)(Version("0.0.0.0"), Version("1.0.0.0"))
    * res14: Either[String,io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType] = Right(Major)
    * }}}
    */
  def fromPreviousAndNextVersion(
    versionCompatibility: VersionCompatibility
  )(previousVersion: Version, nextVersion: Version): Either[String, VersionChangeType] =
    for {
      p      <- NumericVersion.fromCoursierVersion(previousVersion)
      n      <- NumericVersion.fromCoursierVersion(nextVersion)
      result <- fromPreviousAndNextNumericVersion(versionCompatibility)(p, n)
    } yield result

  /** As [[#fromPreviousAndNextVersion]], but using [[NumericVersion]]. */
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

  // Typeclass Instances //

  implicit val orderingInstance: Ordering[VersionChangeType] =
    Ordering.by(_.toString)
}
