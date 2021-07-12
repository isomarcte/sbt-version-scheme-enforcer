package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** A data types which represents a Package Version Policy (PVP) version.
  *
  * PVP version values have similar rules to SemVer version values, but differ
  * in a few key ways.
  *
  * First, PVP version values MAY have any number of numeric components, but
  * the SHOULD have at least 3, though this is not a strict requirement.
  *
  * The first three numeric components are referred to as A.B.C. In the
  * version string "1.2.3", A is 1, B is 2, C is 3. In the version string
  * "4.3.2.1", A is 4, B is 3, C is 2, and 1 is not formally given a name.
  *
  * The first 2 numeric components, A and B, are together referred to as the
  * ''major'' version. For example, in the version string "1.0.0", the major
  * version is "1.0". Similarly, in the version string "3.4.0.0", the major
  * version is "3.4".
  *
  * The third component, C, is referred to as the ''minor'' version.
  *
  * The remaining numeric components after C all together make up the patch
  * version. There is not recommendation or requirement that a PVP version
  * have a patch version, e.g. "1.0.0" is perfectly fine. That said, it is
  * common to see PVP versions which always have exactly 4 numeric components,
  * e.g. "1.2.3.4", but again this is not a formal requirement or explicit
  * recommendation of PVP.
  *
  * As in SemVer, changes to the major version indicate that the API of the
  * project may have been broken, e.g. symbols were removed. Since in PVP the
  * major version has 2 components, unlike the 1 component in SemVer, one can
  * release a new breaking version between any two already released versions
  * that differ in their A value. For example, consider if there are already
  * two release, "1.0.0" and "2.0.0". Under a SemVer scheme, if one would want
  * to release a binary breaking change to the 1.x.x branch that was still <
  * the 2.x.x branch, this would not be possible. However, since PVP considers
  * both A and B to make up the major version one could release "1.1.0". This
  * would indicate a binary breaking change from "1.0.0", while still being
  * less than "2.0.0".
  *
  * As in SemVer, changes to the minor version indicate that the API my
  * include new symbols, but should not include breaking changes. Differing
  * from SemVer is that the minor version is represented by the third number
  * component, C, rather than the second in SemVer, B.
  *
  * @see [[https://pvp.haskell.org]]
  */
sealed abstract class PVPVersion extends Product with Serializable {

  /** The PVP Version number. */
  def versionNumber: NumericSection

  /** Any pre-release version information. For example, "-SNAPSHOT".
    */
  def preReleaseSection: Option[PreReleaseSection]

  /** Any metadata version information. For example "+c018abf".
    */
  def metadataSection: Option[MetadataSection]

  // final //

  private[this] def extractComponentN(n: Int): Option[NumericVersionToken] = versionNumber.value.drop(n).headOption

  /** The A component of the version number. It is recommended, though not
    * required, that all versions have this value.
    */
  final def a: Option[NumericVersionToken] = extractComponentN(0)

  /** The B component of the version number. It is recommended, though not
    * required, that all versions have this value.
    */
  final def b: Option[NumericVersionToken] = extractComponentN(1)

  /** The C component of the version number. It is recommended, though not
    * required, that all versions have this value.
    */
  final def c: Option[NumericVersionToken] = extractComponentN(2)

  /** The components which together make up the major API version, A and
    * B. Typically this will always have a length of 2, but it is valid to
    * have a length of 1 or even 0.
    */
  final def major: Vector[NumericVersionToken] = a.toVector ++ b.toVector

  /** The component which makes up the minor version. This is just an alias for
    * [[#c]]. Versions ''should'' have this, but it is not strictly required.
    */
  final def minor: Option[NumericVersionToken] = c

  /** The patch components of the version number. There is no expectation or
    * recommendation from PVP that this be present, but it usually is.
    */
  final def patch: Vector[NumericVersionToken] = versionNumber.value.drop(3)

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

  /** Convert this [[PVPVersion]] into a [[VersionSections]] value. */
  final def asVersionSections: VersionSections = VersionSections(versionNumber, preReleaseSection, metadataSection)

  /** The representation of as derived from the components.
    *
    * If the version string was parsed directly from
    * [[PVPVersion#fromString]], this should be the same as the original
    * input.
    */
  final def canonicalString: String = asVersionSections.canonicalString

  final override def toString: String = s"PVPVersion(${canonicalString})"
}

object PVPVersion {
  final private[this] case class PVPVersionImpl(
    override val versionNumber: NumericSection,
    override val preReleaseSection: Option[PreReleaseSection],
    override val metadataSection: Option[MetadataSection]
  ) extends PVPVersion

  /** Create a [[PVPVersion]] value from a [[NumericSection]], an optional
    * [[PreReleaseSection]], and an optional [[MetadataSection]].
    *
    * @note It is recommended that the [[NumericSection]] have at least 3
    *       components. It is perfectly acceptable to have more than 3 as
    *       well.
    */
  def apply(
    versionNumber: NumericSection,
    preReleaseSection: Option[PreReleaseSection],
    metadataSection: Option[MetadataSection]
  ): PVPVersion = PVPVersionImpl(versionNumber, preReleaseSection, metadataSection)

  /** Create a [[PVPVersion]] value from a [[VersionSections]].
    *
    * @note It is recommended that the [[NumericSection]] of the
    *       [[VersionSections]] have at least 3 components. It is perfectly
    *       acceptable to have more than 3 as well.
    */
  def fromVersionSections(value: VersionSections): PVPVersion =
    apply(value.numericSection, value.preReleaseSection, value.metadataSection)

  /** Attempt to create a [[PVPVersion]] from a [[java.lang.String]] */
  def fromString(value: String): Either[String, PVPVersion] = VersionSections.fromString(value).map(fromVersionSections)

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): PVPVersion =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  /** This is playing a ''little'' fast and loose with the rules here. In PVP,
    * the ordering ''must'' be the lexicographic ordering of components. The
    * canonical build tool for Haskell, Cabal, is the origin of PVP. Cabal
    * doesn't really have a concept of a pre-release or metadata component as
    * is common in Maven based build systems. Since our representation
    * ''permits'' per-release and metadata values, we do take them into
    * account in the ordering when they are present.
    */
  implicit val orderingInstance: Ordering[PVPVersion] = Ordering.by(_.asVersionSections)

  implicit def versionChangeTypeClassInstance: VersionChangeTypeClass[PVPVersion] =
    new VersionChangeTypeClass[PVPVersion] {
      override def changeType(x: PVPVersion, y: PVPVersion): VersionChangeType =
        if (x.major === y.major) {
          if (x.minor === y.minor) {
            VersionChangeType.Patch
          } else {
            VersionChangeType.Minor
          }
        } else {
          VersionChangeType.Major
        }
    }
}
