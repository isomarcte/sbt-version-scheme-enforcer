package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** A data type which represents an early-semver version value.
  *
  * This is effectively a newtype on [[SemVerVersion]]. The primary difference
  * is that while SemVer states that if the major version is 0, then any
  * change should be considered to be binary breaking, Early SemVer
  * effectively follows PVP for versions < 1.0.0. That is, 0.0.1 -> 0.1.0 is a
  * binary breaking change, 0.0.1 -> 0.0.2 may introduce new
  * symbols. Structurally, it is identical to SemVer.
  *
  * @see [[https://scala-lang.org/blog/2021/02/16/preventing-version-conflicts-with-versionscheme.html Early SemVer]]
  */
sealed abstract class EarlySemVerVersion extends Product with Serializable {

  /** The underlying [[SemVerVersion]]. */
  def value: SemVerVersion

  // final //

  final override def toString: String = s"EarlySemVerVersion(${value.canonicalString})"
}

object EarlySemVerVersion {
  final private[this] case class EarlySemVerVersionImpl(override val value: SemVerVersion) extends EarlySemVerVersion

  /** Create an [[EarlySemVerVersion]] from a [[SemVerVersion]]. */
  def apply(value: SemVerVersion): EarlySemVerVersion = EarlySemVerVersionImpl(value)

  /** Attempt to create a [[SemVerVersion]] from a [[java.lang.String]]. */
  def fromString(value: String): Either[String, EarlySemVerVersion] = SemVerVersion.fromString(value).map(apply)

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): EarlySemVerVersion =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  implicit val orderingInstance: Ordering[EarlySemVerVersion] = Ordering.by(_.value)

  implicit def versionChangeTypeClassInstance: VersionChangeTypeClass[EarlySemVerVersion] =
    new VersionChangeTypeClass[EarlySemVerVersion] {
      override def changeType(x: EarlySemVerVersion, y: EarlySemVerVersion): VersionChangeType =
        if (x.value.major === 0 && y.value.major === 0) {
          // PVP rules
          VersionChangeTypeClass[PVPVersion].changeType(
            PVPVersion.fromVersionSections(x.value.asVersionSections),
            PVPVersion.fromVersionSections(y.value.asVersionSections)
          )
        } else {
          // SemVer Rules
          VersionChangeTypeClass[SemVerVersion].changeType(x.value, y.value)
        }
    }
}
