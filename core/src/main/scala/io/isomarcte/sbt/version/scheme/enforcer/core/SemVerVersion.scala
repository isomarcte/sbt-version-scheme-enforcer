package io.isomarcte.sbt.version.scheme.enforcer.core

/** A data type which represents a SemVer version.
  *
  * @note This value is special in one regard. The SemVer specification states
  *       that the metadata MUST NOT be taken into account when determining
  *       version precedence. However implicit [[Ordering]] instance for this
  *       type ''does'' return non-equal for two SemVer values which differ
  *       only in their metadata. This is by design, so that instances of this
  *       value behave as expected in hash and order based data structures,
  *       e.g. [[Set]] or [[SortedSet]]. This library ''does'' treat two
  *       instances of this type which only differ in their API as having the
  *       same logical precedence with respect to the API version, it is
  *       merely that the implicit [[Ordering]] is not ordering by that
  *       precedence. Usually none of this will matter to you. If you do have
  *       a need to use an [[Ordering]] which is defined in terms of the
  *       SemVer logical API precedence, then you can either use the
  *       [[SemVerVersion#semVerPrecedenceVersion]] component of this type which does
  *       not include the metadata ''or'' use the non-implicit ordering
  *       [[SemVerVersion#semVerPrecedenceOrdering]].
  *
  * @see [[https://semver.org/ SemVer]]
  */
sealed abstract class SemVerVersion extends Product with Serializable {

  /** The [[SemVerPrecedenceVersion]] which is the sub-part of a SemVer version
    * which denotes specific logical precedence between versions according
    * specification.
    */
  def semVerPrecedenceVersion: SemVerPrecedenceVersion

  /** The [[MetadataSection]] of the version.
    *
    * @note The SemVer specifications states that this should be ignored when
    * determining logical precedence between versions.
    */
  def metadataSection: Option[MetadataSection]

  // final //

  /** The major version number. According to the SemVer specification, a
    * difference in the major version number may indicate that the two
    * versions are binary incompatible (e.g. symbols were removed).
    *
    * Additionally, if the major version number is 0, then any difference in
    * the version numbers can indicate a binary incompatible change.
    */
  final def major: BigInt = semVerPrecedenceVersion.major

  /** The minor version number. According to the SemVer specification, a
    * difference in the minor version number indicates that new publicly
    * visible symbols may be present, but that no symbols were removed. In
    * other words it's not a binary breaking change.
    *
    * @note There is an exception to this when the [[#major]] version is 0. In
    *       that case, any difference in the version number may indicate a
    *       binary breaking change.
    */
  final def minor: BigInt = semVerPrecedenceVersion.minor

  /** The patch version number. According to the SemVer specification, a
    * difference in the patch version number indicates that the visible API
    * has not changed at all. The changes should thus be both binary and
    * source compatible.
    *
    * @note There is an exception to this when the [[#major]] version is 0. In
    *       that case, any difference in the version number may indicate a
    *       binary breaking change.
    */
  final def patch: BigInt = semVerPrecedenceVersion.patch

  /** The pre-release section of the version.
    *
    * Common examples of this include `-SNAPSHOT` or `-RC1`.
    */
  final def preReleaseSection: Option[PreReleaseSection] = semVerPrecedenceVersion.preReleaseSection

  /** Does this version represent a pre-release?
    *
    * This is shorthand for `_.preReleaseSection.isDefined`.
    */
  final def isPreRelease: Boolean = semVerPrecedenceVersion.isPreRelease

  /** Is this version a SNAPSHOT?
    *
    * This is shorthand for `_.preReleaseSection ==
    * Some(PreReleaseSection.snapshot)`.
    */
  final def isSnapshot: Boolean = semVerPrecedenceVersion.isSnapshot

  /** The representation of as derived from the components.
    *
    * If the version string was parsed directly from
    * [[SemVerVersion#fromString]], this should be the same as the original
    * input.
    */
  final def canonicalString: String =
    s"""${semVerPrecedenceVersion.canonicalString}${metadataSection.fold("")(_.canonicalString)}"""

  /** Convert this [[SemVerPrecedenceVersion]] into a [[VersionSections]] value. */
  final def asVersionSections: VersionSections =
    semVerPrecedenceVersion.asVersionSections.withMetadataSection(metadataSection)

  final override def toString: String = s"SemVerVersion(${canonicalString})"
}

object SemVerVersion {
  final private[this] case class SemVerVersionImpl(
    override val semVerPrecedenceVersion: SemVerPrecedenceVersion,
    override val metadataSection: Option[MetadataSection]
  ) extends SemVerVersion

  /** Create a [[SemVerVersion]] from a [[SemVerPrecedenceVersion]] and [[MetadataSection]]. */
  def apply(semVerPrecedenceVersion: SemVerPrecedenceVersion, metadataSection: Option[MetadataSection]): SemVerVersion =
    SemVerVersionImpl(semVerPrecedenceVersion, metadataSection)

  /** Create a [[SemVerVersion]] from a [[SemVerPrecedenceVersion]] and [[MetadataSection]]. */
  def apply(semVerPrecedenceVersion: SemVerPrecedenceVersion, metadataSection: MetadataSection): SemVerVersion =
    apply(semVerPrecedenceVersion, Some(metadataSection))

  /** Create a [[SemVerVersion]] from a [[SemVerPrecedenceVersion]]. */
  def apply(semVerPrecedenceVersion: SemVerPrecedenceVersion): SemVerVersion = apply(semVerPrecedenceVersion, None)

  /** Attempt to create a [[SemVerVersion]] from [[BigInt]] values representing
    * the major, minor, and patch versions and from a [[PreReleaseSection]]
    * and [[MetadataSection]].
    *
    * The [[BigInt]] values must be >= 0.
    */
  def from(
    major: BigInt,
    minor: BigInt,
    patch: BigInt,
    preReleaseSection: Option[PreReleaseSection],
    metadataSection: Option[MetadataSection]
  ): Either[String, SemVerVersion] =
    SemVerPrecedenceVersion
      .from(major, minor, patch, preReleaseSection)
      .fold(
        e =>
          Left(
            s"""Error parsing version ${major}.${minor}.${patch}${preReleaseSection
              .fold("")(_.canonicalString)}${metadataSection.fold("")(_.canonicalString)}, ${e}"""
          ),
        value => Right(apply(value, metadataSection))
      )

  /** As [[#from]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFrom(
    major: BigInt,
    minor: BigInt,
    patch: BigInt,
    preReleaseSection: Option[PreReleaseSection],
    metadataSection: Option[MetadataSection]
  ): SemVerVersion =
    from(major, minor, patch, preReleaseSection, metadataSection)
      .fold(e => throw new IllegalArgumentException(e), identity)

  /** Attempt to create a [[SemVerVersion]] from a [[VersionSections]] value.
    *
    * If there are not exactly 3 numeric components this will fail.
    */
  def fromSections(versionSections: VersionSections): Either[String, SemVerVersion] =
    SemVerPrecedenceVersion
      .fromSections(versionSections.numericSection, versionSections.preReleaseSection)
      .fold(
        e => Left(s"""Error parsing version from sections ${versionSections}, ${e}"""),
        value => Right(apply(value, versionSections.metadataSection))
      )

  /** As [[#fromSections]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFromSections(versionSections: VersionSections): SemVerVersion =
    fromSections(versionSections).fold(e => throw new IllegalArgumentException(e), identity)

  /** Attempt to create a [[SemVerVersion]] from a [[java.lang.String]]. */
  def fromString(value: String): Either[String, SemVerVersion] = VersionSections.fromString(value).flatMap(fromSections)

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): SemVerVersion =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  implicit val orderingInstance: Ordering[SemVerVersion] = Ordering.by(_.asVersionSections)

  /** A special [[Ordering]] instance which will compare ''exactly'' according
    * to the precedence rules defined in SemVer. You are unlikely to need this
    * value.
    *
    * @note This [[Ordering]] will ''not'' inspect the [[MetadataSection]],
    *       and thus when `_.compare(a, b) == 0`, that does ''not'' mean that
    *       `a == b` or `a.hashCode == b.hashCode`.
    */
  val semVerPrecedenceOrdering: Ordering[SemVerVersion] = Ordering.by(_.semVerPrecedenceVersion)
}
