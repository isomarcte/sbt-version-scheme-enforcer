package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** A data type which represents the part of a SemVer version which denotes
  * precedence. In other words, it is a SemVer version without the
  * [[MetadataSection]].
  *
  * According to the SemVer specification (section 10), metadata MUST be
  * ignored when taking into account version precedence. The reason that this
  * is represented in an isolated data type is that we ''do'' want to take the
  * [[MetadataSection]] into account on the [[SemVerVersion]] data type. If we
  * didn't take it into account, then either our `Ordering` instance would
  * have to violate it's contract ''or'' versions which differ in terms of
  * their metadata would have to compare equal.
  *
  * @see [[https://semver.org/ SemVer]]
  */
sealed abstract class SemVerPrecedenceVersion extends Product with Serializable {

  /** The [[NumericVersionToken]] which represents the "major" version number. */
  def majorToken: NumericVersionToken

  /** The [[NumericVersionToken]] which represents the "minor" version number. */
  def minorToken: NumericVersionToken

  /** The [[NumericVersionToken]] which represents the "patch" version number. */
  def patchToken: NumericVersionToken

  /** An optional [[PreReleaseSection]]. */
  def preReleaseSection: Option[PreReleaseSection]

  // final //

  /** The major version number. According to the SemVer specification, a
    * difference in the major version number may indicate that the two
    * versions are binary incompatible (e.g. symbols were removed).
    *
    * Additionally, if the major version number is 0, then any difference in
    * the version numbers can indicate a binary incompatible change.
    */
  final def major: BigInt = majorToken.value

  /** The minor version number. According to the SemVer specification, a
    * difference in the minor version number indicates that new publicly
    * visible symbols may be present, but that no symbols were removed. In
    * other words it's not a binary breaking change.
    *
    * @note There is an exception to this when the [[#major]] version is 0. In
    *       that case, any difference in the version number may indicate a
    *       binary breaking change.
    */
  final def minor: BigInt = minorToken.value

  /** The patch version number. According to the SemVer specification, a
    * difference in the patch version number indicates that the visible API
    * has not changed at all. The changes should thus be both binary and
    * source compatible.
    *
    * @note There is an exception to this when the [[#major]] version is 0. In
    *       that case, any difference in the version number may indicate a
    *       binary breaking change.
    */
  final def patch: BigInt = patchToken.value

  /** The representation as derived from the components.
    *
    * If the version string was parsed directly from
    * [[SemVerPrecedenceVersion#fromString]], this should be the same as the
    * original input.
    */
  final def canonicalString: String = s"""${major}.${minor}.${patch}${preReleaseSection.fold("")(_.canonicalString)}"""

  /** Does this version represent a pre-release?
    *
    * This is shorthand for `_.preReleaseSection.isDefined`.
    */
  final def isPreRelease: Boolean = preReleaseSection.isDefined

  /** Is this version a SNAPSHOT?
    *
    * This is shorthand for `_.preReleaseSection ==
    * Some(PreReleaseSection.snapshot)`.
    */
  final def isSnapshot: Boolean = preReleaseSection === Some(PreReleaseSection.snapshot)

  /** Convert this [[SemVerPrecedenceVersion]] into a [[VersionSections]] value.
    *
    * @note This result will ''never'' have a [[MetadataSection]].
    */
  final def asVersionSections: VersionSections =
    VersionSections(NumericSection(Vector(majorToken, minorToken, patchToken)), preReleaseSection, None)

  final override def toString: String = s"SemVerPrecedenceVersion(${canonicalString})"
}

object SemVerPrecedenceVersion {
  final private[this] case class SemVerPrecedenceVersionImpl(
    override val majorToken: NumericVersionToken,
    override val minorToken: NumericVersionToken,
    override val patchToken: NumericVersionToken,
    override val preReleaseSection: Option[PreReleaseSection]
  ) extends SemVerPrecedenceVersion

  /** Create a [[SemVerPrecedenceVersion]] from [[NumericVersionToken]] values
    * representing the major, minor, and patch version numbers, as well as an
    * optional [[PreReleaseSection]] value.
    *
    * This is always safe because [[NumericVersionToken]] values are already
    * validated to be >= 0.
    */
  def apply(
    major: NumericVersionToken,
    minor: NumericVersionToken,
    patch: NumericVersionToken,
    preReleaseSection: Option[PreReleaseSection]
  ): SemVerPrecedenceVersion = SemVerPrecedenceVersionImpl(major, minor, patch, preReleaseSection)

  /** Attempt to create a [[SemVerPrecedenceVersion]] from [[BigInt]] values
    * representing the major, minor, and patch version numbers, as well as an
    * optional [[PreReleaseSection]].
    *
    * If any of the version numbers are < 0 this will fail.
    */
  def from(
    major: BigInt,
    minor: BigInt,
    patch: BigInt,
    preReleaseSection: Option[PreReleaseSection]
  ): Either[String, SemVerPrecedenceVersion] =
    for {
      majorToken <- NumericVersionToken.fromBigInt(major)
      minorToken <- NumericVersionToken.fromBigInt(minor)
      patchToken <- NumericVersionToken.fromBigInt(patch)
    } yield apply(majorToken, minorToken, patchToken, preReleaseSection)

  /** As [[#from]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFrom(
    major: BigInt,
    minor: BigInt,
    patch: BigInt,
    preReleaseSection: Option[PreReleaseSection]
  ): SemVerPrecedenceVersion =
    from(major, minor, patch, preReleaseSection).fold(e => throw new IllegalArgumentException(e), identity)

  /** Attempt to create a [[SemVerPrecedenceVersion]] from a [[NumericSection]]
    * and an optional [[PreReleaseSection]].
    *
    * This will fail if the [[NumericSection]] does not have ''exactly'' 3
    * components.
    */
  def fromSections(
    numericSection: NumericSection,
    preReleaseSection: Option[PreReleaseSection]
  ): Either[String, SemVerPrecedenceVersion] =
    if (numericSection.value.size === 3) {
      from(
        numericSection.value(0).value,
        numericSection.value(1).value,
        numericSection.value(2).value,
        preReleaseSection
      )
    } else {
      Left(
        s"SemVer versions must have exactly three numeric version components, found ${numericSection.value.size}: ${numericSection}"
      )
    }

  /** Attempt to create a [[SemVerPrecedenceVersion]] from a
    * [[java.lang.String]] value.
    */
  def fromString(value: String): Either[String, SemVerPrecedenceVersion] =
    VersionSections
      .fromString(value)
      .flatMap(sections =>
        if (sections.metadataSection.isDefined) {
          Left(
            s"""SemVerPrecedenceVersion can not have a metadata section, but one was found. You probably want to be using SemVerVersion instead of SemVerPrecedenceVersion: ${value} ${sections
              .metadataSection
              .fold("")(value => "(metadata is " + value.canonicalString + ")")}"""
          )
        } else {
          fromSections(sections.numericSection, sections.preReleaseSection)
        }
      )

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): SemVerPrecedenceVersion =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  implicit val orderingInstance: Ordering[SemVerPrecedenceVersion] = Ordering.by(_.asVersionSections)
}
