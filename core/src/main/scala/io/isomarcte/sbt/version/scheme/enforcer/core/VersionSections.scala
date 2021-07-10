package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** A data type which represents a common encoding of the components of a
  * version string.
  *
  * - Version String: A version string is some string which uniquely
  *   identifies a version. It commonly contains up to three ''sections''.
  * - Version Section: A version section is a part of a version string which
  *   communicates something about the project. The common sections are the
  *   numeric section (which is what most people think of as the version
  *   number), the pre-release section (well known examples are `-SNAPSHOT` or
  *   `-RC1`), and the metadata section (for example, the VCS commit
  *   reference). Each section can have zero or more ''components''.
  * - Numeric Section: The numeric section is the part of the version string
  *   which communicates what people commonly refer to as the "version
  *   number". It often communicates something about the project relative to
  *   other version numbers. For example, if a project is following the
  *   SemVer, Early SemVer, or PVP versioning schemes, then the number
  *   communicates something about the particular binary API exposed by the
  *   project. For PVP, it can also communicate a few other notions, such as
  *   the addition of orphan typeclass instances or the deprecation of
  *   symbols (methods/functions/classes/traits/etc.).
  * - Pre-Release Section: The pre-release section is the part of the version
  *   string which communicates whether or not a particular version of a
  *   project has been "fully released". A pre-release section is the first
  *   string which starts with '-' and continues as long as each character is
  *   one of `[0-9A-Za-z-.]`. Typically it either continues to the end of the
  *   string, or when there is metadata, until the first '+' character.  There
  *   are few well defined rules about what that means for the actual
  *   project. Some projects maintain binary compatibility between
  *   pre-releases of the same version, while others don't. It's utility is
  *   thus mostly useful for denoting that a particular pre-release is further
  *   along than some other pre-release and as a message to the users that the
  *   project at this version is not quite stable. For example, in the
  *   versions "1.0.0-SNAPSHOT", "1.0.0-RC1", and "1.0.0-RC10", the
  *   pre-release sections are "-SNAPSHOT", "-RC1", "-RC10".
  * - Metadata Section: The metadata section always follows the numeric and
  *   pre-release sections, if they are present. It begins with the first '+'
  *   character in the version string and continues as long as each character
  *   is one of `[0-9A-Za-z-.]`. What it specifically communicates is
  *   undefined, though common examples are the build date or commit
  *   reference.
  *
  * The encoding of version sections in this data type is generally based off
  * of the SemVer specification, though it can be compatible with other
  * version schemes. There are some caveats to this, so please consult the
  * ScalaDoc for specific version scheme data types.
  *
  * It defines a [[NumericSection]], which is followed by a
  * [[PreReleaseSection]], which is followed by a [[MetadataSection]], any of
  * which may be empty ''in this datatype'', but a particular version scheme
  * type may disallow this. For example, in SemVer and Early SemVer the
  * [[NumericSection]] may ''not'' be empty and ''must'' contain exactly three
  * components. For the [[PreReleaseSection]] and the [[MetadataSection]],
  * they may be empty in two distinct ways. They are each defined to start
  * with a special character in the version string, '-' for the
  * [[PreReleaseSection]] and '+' for the [[MetadataSection]]. If these
  * characters are missing, then they can be missing as encoded by a `None`
  * value. On the other hand, if these characters are present, then they
  * ''can''' be `Some` values, but if they have no components (which is
  * legal), they will be empty in that regard.
  *
  * For example,
  * {{{
  * scala> VersionSections.unsafeFromString("1.0.0").preReleaseSection.map(_.canonicalString)
  * res0: Option[String] = None
  *
  * scala> VersionSections.unsafeFromString("1.0.0-").preReleaseSection.map(_.canonicalString)
  * res1: Option[String] = Some(-)
  *
  * scala> VersionSections.unsafeFromString("1.0.0-").preReleaseSection == Some(PreReleaseSection.empty)
  * res2: Boolean = true
  *
  * scala> VersionSections.unsafeFromString("1.0.0").metadataSection.map(_.canonicalString)
  * res3: Option[String] = None
  *
  * scala> VersionSections.unsafeFromString("1.0.0+").metadataSection.map(_.canonicalString)
  * res4: Option[String] = Some(+)
  * }}}
  *
  * @example In the version string `"1.0.0-RC1+d989857"`, the
  *          [[NumericSection]] is `"1.0.0"`, the [[PreReleaseSection]] is
  *          "-RC1", and the [[MetadataSection]] is "+d989857".
  *
  * @note [[VersionSections]] provides an [[Ordering]] instance, which works
  *       ''generally'' how one would expect. That said, the ''specific''
  *       ordering of a set of version components is dependent on the version
  *       scheme in use. Thus, this [[Ordering]] is mostly here to permit
  *       using [[VersionSections]] values in data structures which require
  *       ordering, e.g. [[scala.collection.immutable.SortedSet]]. For
  *       actually comparing versions, you should use the version scheme
  *       specific data types.
  *
  * @see [[https://semver.org/ SemVer]]
  * @see [[https://pvp.haskell.org/ PVP]]
  */
sealed abstract class VersionSections extends Product with Serializable {

  /** The numeric section of the version string. The [[NumericSection]] is
    * ''always'' present, even if it is the empty string. For example, in the
    * (odd) version string "-SNAPSHOT", the [[NumericSection]] value's string
    * representation is "", equating to [[NumericSection#empty]].
    *
    * @example In the version string "1.0.0-SNAPSHOT+d989857", this is
    *          "1.0.0".
    */
  def numericSection: NumericSection

  /** The pre-release section of the version string.
    *
    * @note This section may be empty in two different ways. The section is
    *       must always begin with a '-' character. This character is not part
    *       of any pre-release component, it just denotes the start of the
    *       pre-release section. After parsing numeric section, if the end of
    *       input is reach or the start of the metadata section is reached,
    *       then there is no '-' character to start the pre-release
    *       section. In that case, this value will be `None`. Alternatively,
    *       if the '-' character ''is'' present, but there are no components
    *       after it, then this value will be `Some(PreReleaseSection.empty)`.
    *
    * @example In the version string "1.0.0-SNAPSHOT+d989857", this is
    *          "-SNAPSHOT".
    */
  def preReleaseSection: Option[PreReleaseSection]

  /** The metadata section of the version string. May be empty.
    *
    * @example In the version string "1.0.0-SNAPSHOT+d989857", this is
    *          "+d989857".
    */
  def metadataSection: Option[MetadataSection]

  // final //

  final def withNumericSection(value: NumericSection): VersionSections =
    VersionSections(value, preReleaseSection, metadataSection)

  final def withPreReleaseSection(value: Option[PreReleaseSection]): VersionSections =
    VersionSections(numericSection, value, metadataSection)

  final def withMetadataSection(value: Option[MetadataSection]): VersionSections =
    VersionSections(numericSection, preReleaseSection, value)

  /** The representation of the version string as derived from the components.
    *
    * If the version string was parsed directly from
    * [[VersionSections#fromString]], this should be the same as the
    * original version string.
    */
  final def canonicalString: String = {
    val preReleaseSectionString: String = preReleaseSection.fold("")(_.canonicalString)
    val metadataSectionString: String   = metadataSection.fold("")(_.canonicalString)

    s"${numericSection.canonicalString}${preReleaseSectionString}${metadataSectionString}"
  }

  final override def toString: String =
    s"VersionSections(numericSection = ${numericSection}, preReleaseSection = ${preReleaseSection}, metadataSection = ${metadataSection})"
}

object VersionSections {
  final private[this] case class VersionSectionsImpl(
    override val numericSection: NumericSection,
    override val preReleaseSection: Option[PreReleaseSection],
    override val metadataSection: Option[MetadataSection]
  ) extends VersionSections

  /** The empty [[VersionSections]].
    *
    * This is effectively the empty string "".
    *
    * @note This is ''not'' the same as
    *       `VersionSections(NumericSection.empty, PreReleaseSection.empty,
    *       MetadataSection.empty)`, which would be equivalent to the
    *       (unusual) version string "-+".
    */
  val empty: VersionSections = VersionSectionsImpl(NumericSection.empty, None, None)

  /** Attempt to parse a [[VersionSections]] from a [[java.lang.String]].
    *
    * @note The presence of leading or trailing whitespace will cause this to
    *       fail. If you expect that to be typical, you should trim the value
    *       before invoking this.
    */
  def fromString(value: String): Either[String, VersionSections] = {
    val (numeric, rest0)       = value.span(char => char =!= '-' && char =!= '+')
    val (preRelease, metadata) = rest0.span(_ =!= '+')

    val preReleaseSection: Either[String, Option[PreReleaseSection]] =
      if (preRelease.isEmpty) {
        Right(None)
      } else {
        PreReleaseSection.fromString(preRelease).map(Option.apply)
      }

    val metadataSection: Either[String, Option[MetadataSection]] =
      if (metadata.isEmpty) {
        Right(None)
      } else {
        MetadataSection.fromString(metadata).map(Option.apply)
      }

    for {
      numericSection <- NumericSection.fromString(numeric)
      preRelease     <- preReleaseSection
      metadata       <- metadataSection
    } yield apply(numericSection, preRelease, metadata)
  }

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): VersionSections =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  implicit val ordering: Ordering[VersionSections] =
    new Ordering[VersionSections] {
      override def compare(x: VersionSections, y: VersionSections): Int =
        Ordering[NumericSection].compare(x.numericSection, y.numericSection) match {
          case 0 =>
            (x.preReleaseSection, y.preReleaseSection) match {
              // If there is no pre-release section that has a greater order
              // than if there is one, e.g. 1.0.0 > 1.0.0-RC1. So these are
              // flipped from how they might intuitively be computed.
              case (Some(_), None) =>
                -1
              case (None, Some(_)) =>
                1
              case (a, b) =>
                a.flatMap(a => b.map(b => Ordering[PreReleaseSection].compare(a, b))).getOrElse(0) match {
                  case 0 =>
                    (x.metadataSection, y.metadataSection) match {
                      case (Some(a), Some(b)) =>
                        Ordering[MetadataSection].compare(a, b)
                      case (Some(_), None) =>
                        1
                      case (None, Some(_)) =>
                        -1
                      case (None, None) =>
                        0
                    }
                  case otherwise =>
                    otherwise
                }
            }

          case otherwise =>
            otherwise
        }
    }

  // Begin a whole lot of boring constructors

  // Arity 3

  /** Create a [[VersionSections]] from the [[NumericSection]], an optional
    * [[PreReleaseSection]] value, and an optional [[MetadataSection]] value.
    *
    * This is the most general construction form.
    */
  def apply(
    numericSection: NumericSection,
    preReleaseSection: Option[PreReleaseSection],
    metadataSection: Option[MetadataSection]
  ): VersionSections = VersionSectionsImpl(numericSection, preReleaseSection, metadataSection)

  /** Create a [[VersionSections]] from the [[NumericSection]], a
    * [[PreReleaseSection]] value, and an optional [[MetadataSection]] value.
    *
    * This is the most general construction form.
    */
  def apply(
    numericSection: NumericSection,
    preReleaseSection: PreReleaseSection,
    metadataSection: Option[MetadataSection]
  ): VersionSections = VersionSectionsImpl(numericSection, Some(preReleaseSection), metadataSection)

  /** Create a [[VersionSections]] from the [[NumericSection]], an optional
    * [[PreReleaseSection]] value, and a [[MetadataSection]] value.
    *
    * This is the most general construction form.
    */
  def apply(
    numericSection: NumericSection,
    preReleaseSection: Option[PreReleaseSection],
    metadataSection: MetadataSection
  ): VersionSections = VersionSectionsImpl(numericSection, preReleaseSection, Some(metadataSection))

  /** Create a [[VersionSections]] from the [[NumericSection]],
    * [[PreReleaseSection]], and the [[MetadataSection]].
    */
  def apply(
    numericSection: NumericSection,
    preReleaseSection: PreReleaseSection,
    metadataSection: MetadataSection
  ): VersionSections = VersionSectionsImpl(numericSection, Some(preReleaseSection), Some(metadataSection))

  // Arity 2

  /** Create a [[VersionSections]] from the [[NumericSection]] and
    * [[PreReleaseSection]]. The [[MetadataSection]] is `None`.
    */
  def apply(numericSection: NumericSection, preReleaseSection: PreReleaseSection): VersionSections =
    VersionSectionsImpl(numericSection, Some(preReleaseSection), None)

  /** Create a [[VersionSections]] from the [[NumericSection]] and
    * [[MetadataSection]]. The [[PreReleaseSection]] is `None`.
    */
  def apply(numericSection: NumericSection, metadataSection: MetadataSection): VersionSections =
    VersionSectionsImpl(numericSection, None, Some(metadataSection))

  /** Create a [[VersionSections]] from a [[PreReleaseSection]] value and a
    * [[MetadataSection]] value. The [[NumericSection]] is empty.
    */
  def apply(preReleaseSection: PreReleaseSection, metadataSection: MetadataSection): VersionSections =
    VersionSectionsImpl(NumericSection.empty, Some(preReleaseSection), Some(metadataSection))

  // Arity 1

  /** Create a [[VersionSections]] from the [[NumericSection]]. The
    * [[PreReleaseSection]] and [[MetadataSection]] values are None.
    */
  def apply(numericSection: NumericSection): VersionSections = VersionSectionsImpl(numericSection, None, None)

  /** Create a [[VersionSections]] from the [[PreReleaseSection]]. The
    * [[NumericSection]] is empty and the [[MetadataSection]] value is `None`.
    */
  def apply(preReleaseSection: PreReleaseSection): VersionSections =
    VersionSectionsImpl(NumericSection.empty, Some(preReleaseSection), None)

  /** Create a [[VersionSections]] from the [[MetadataSection]]. The
    * [[NumericSection]] is empty and the [[PreReleaseSection]] value is `None`.
    */
  def apply(metadataSection: MetadataSection): VersionSections =
    VersionSectionsImpl(NumericSection.empty, None, Some(metadataSection))
}
