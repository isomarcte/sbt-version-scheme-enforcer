package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import coursier.version.{Version => CVersion}

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
  @deprecated(message = "Please use VersionChangeTypeClass.changeType instances instead.", since = "2.1.1.0")
  def fromPreviousAndNextVersion(
    versionCompatibility: VersionCompatibility
  )(previousVersion: CVersion, nextVersion: CVersion): Either[String, VersionChangeType] =
    versionCompatibility match {
      case VersionCompatibility.PackVer | VersionCompatibility.Default =>
        for {
          x <- PVPVersion.fromString(previousVersion.repr)
          y <- PVPVersion.fromString(nextVersion.repr)
        } yield VersionChangeTypeClass[PVPVersion].changeType(x, y)
      case VersionCompatibility.SemVerSpec =>
        for {
          x <- SemVerVersion.fromString(previousVersion.repr)
          y <- SemVerVersion.fromString(nextVersion.repr)
        } yield VersionChangeTypeClass[SemVerVersion].changeType(x, y)
      case VersionCompatibility.EarlySemVer | VersionCompatibility.SemVer =>
        for {
          x <- EarlySemVerVersion.fromString(previousVersion.repr)
          y <- EarlySemVerVersion.fromString(nextVersion.repr)
        } yield VersionChangeTypeClass[EarlySemVerVersion].changeType(x, y)
      case VersionCompatibility.Strict =>
        Right(VersionChangeTypeClass[StrictVersion].changeType(StrictVersion(previousVersion.repr), StrictVersion(nextVersion.repr)))
      case VersionCompatibility.Always =>
        Right(VersionChangeTypeClass[AlwaysVersion].changeType(AlwaysVersion(previousVersion.repr), AlwaysVersion(nextVersion.repr)))
    }

  /** As [[#fromPreviousAndNextVersion]], but using [[NumericVersion]]. */
  @deprecated(message = "Please use VersionChangeTypeClass.changeType instances instead.", since = "2.1.1.0")
  def fromPreviousAndNextNumericVersion(
    versionCompatibility: VersionCompatibility
  )(previousVersion: NumericVersion, nextVersion: NumericVersion): Either[String, VersionChangeType] =
    fromPreviousAndNextVersion(
      versionCompatibility
    )(CVersion(previousVersion.versionString), CVersion(nextVersion.versionString))

  // Typeclass Instances //

  implicit val orderingInstance: Ordering[VersionChangeType] = Ordering.by(_.toString)
}
