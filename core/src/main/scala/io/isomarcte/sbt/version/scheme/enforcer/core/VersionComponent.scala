package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import scala.util.Try
import scala.util.matching.Regex

/** A Algebraic Data Type (ADT) to model a component of a version number.
  *
  * A version string is made up of multiple sections, each of which may have
  * some number components.
  *
  * For example, consider the version string "1.0.0-SNAPSHOT". This version
  * string has two ''sections'', a version number ("1.0.0") and a pre-release
  * annotation ("SNAPSHOT"). Within the version number section, there are
  * three ''components'' 1, 0, and 0, separated by '.' characters.
  *
  * @note Not all version schemes support the same notion of
  *       components. Whether or not a particular set of components is valid
  *       for a particular version string is dependent on the version scheme
  *       in use.
  *
  * @note For the purposes of parsing component values we use SemVer's
  *       definition. This is because of the components shared between schemes
  *       SemVer's encoding is compatible with the other supported schemes.
  *
  * @note [[VersionComponent]]'s do implement [[Ordered]], but this isn't
  *       ordering as one might commonly associate with version strings. It
  *       exists merely for use with order dependent collections, such as
  *       [[scala.collections.immutable.SortedSet]]. For ordering a version
  *       string, you should use the implementations for the specific version
  *       types.
  *
  * @see [[https://semver.org/]]
  */
sealed abstract class VersionComponent extends Product with Serializable with Ordered[VersionComponent] {
  import VersionComponent._

  /** The [[java.lang.String]] representation of the component.
    *
    * @note This value ''will never'' be modified during parsing. Some
    *       components provide more convenient parsed representations of this
    *       value, but if parsing is successful this ''will'' be the input
    *       value.
    */
  def value: String

  // Final //

  final override def compare(that: VersionComponent): Int =
    (this, that) match {
      case (a: VersionNumberComponent, b: VersionNumberComponent) =>
        a.value.compare(b.value)
      case (a: PreRelease, b: PreRelease) =>
        a.value.compare(b.value)
      case (a: Metadata, b: Metadata) =>
        a.value.compare(b.value)
      case (_: VersionNumberComponent, _) =>
        1
      case (_: PreRelease, _: VersionNumberComponent) =>
        -1
      case (_: PreRelease, _) =>
        1
      case (_: Metadata, _) =>
        -1
    }
}

object VersionComponent {

  /** A regular expression for matching non-numeric pre-release values according
    * to SemVer. That is, if the component is not entirely numeric (it
    * contains one more letters or - characters), then it must match this
    * pattern.
    *
    * If the component only contains digits, it must match
    * [[#numericNoLeadingZerosRegex]].
    */
  private[this] val preReleaseValidNonNumericRegex: Regex = """^[0-9A-Za-z-]+$""".r

  /** A regular expression for matching non-numeric metadata values according to
    * SemVer.
    */
  private[this] def metadataValidNonNumericRegex: Regex = preReleaseValidNonNumericRegex

  /** A regular expression for matching numeric strings with no leading
    * zeros. That is, if the first character is 0, then that must be the
    * ''only'' character.
    *
    * These are valid, "0", "10". This is invalid, "010".
    */
  private[this] val numericNoLeadingZerosRegex: Regex = """^(0|[1-9][0-9]*)$""".r

  /** A [[Regex]] which matches any [[java.lang.String]] of only digits.
    */
  private[this] val numericRegex: Regex = """\d+""".r

  /** A predicate to test if a character is a valid pre-release or metadata ''section'' character.
    *
    * @note This is ''not'' the same as a valid pre-release or metadata
    *       ''component'' character. The difference is that we also permit '.'
    *       (the component separator) value.
    */
  private[this] val preReleaseAndMetadataValidCharPred: Char => Boolean =
    (c: Char) => (c.isLetterOrDigit || c === '.' || c === '-')

  /** Common [[java.lang.String]] used in various error messages. */
  private[this] val whitespaceWarningString: String =
    "They _can not_ include whitespace. If you are trying to parse a version string which may have leading or trailing whitespace you should be using VersionComponent.fromVersionString rather than the individual component methods."

  /** Checks if a given [[java.lang.String]] is structurally a valid pre-release
    * component.
    */
  private[this] def isValidPreReleaseString(value: String): Boolean =
    if (numericRegex.pattern.matcher(value).matches) {
      // It is important that we do _not_ test the
      // preReleaseValidNonNumericRegex if the entire string is numeric. If we
      // did, we would permit leading zeros, which is not legal.
      numericNoLeadingZerosRegex.pattern.matcher(value).matches
    } else {
      preReleaseValidNonNumericRegex.pattern.matcher(value).matches
    }

  /** A [[VersionComponent]] which represents a numeric version number
    * component. Zero or more of these make up the version ''number'' in a
    * version string.
    *
    * @note This is defined to be a non-negative integral value. This implies
    *       that the must have a valid [[BigInt]] representation, though as
    *       will all [[VersionComponent]] constructors the underlying
    *       [[VersionComponent#value]] is never modified from input. You may
    *       use [[VersionNumberComponent#asBigInt]] to get access to a numeric
    *       view of the component.
    *
    * @note Leading `"0"` values are not valid according to SemVer.
    */
  sealed abstract class VersionNumberComponent extends VersionComponent {

    /** The version component string, represented as a [[BigInt]].
      */
    def asBigInt: BigInt

    // Final //

    final override def toString: String = s"VersionNumberComponent(value = ${value}, asBigInt = ${asBigInt})"
  }

  object VersionNumberComponent {
    final private[this] case class VersionNumberComponentImpl(override val value: String, override val asBigInt: BigInt)
        extends VersionNumberComponent

    /** Attempt to create a [[VersionComponent#VersionNumberComponent]] from an
      * arbitrary [[java.lang.String]].
      */
    def fromString(value: String): Either[String, VersionNumberComponent] =
      if (value.isEmpty) {
        Left("Can not creat VersionNumberComponent VersionComponent from empty value."): Either[
          String,
          VersionNumberComponent
        ]
      } else if (numericNoLeadingZerosRegex.pattern.matcher(value).matches) {
        Try(BigInt(value))
          .toEither
          .fold(
            e =>
              Left(
                s"Failed when attempting to parse ${value} as non-negative integral value: ${e.getLocalizedMessage}"
              ),
            bigInt =>
              if (bigInt < BigInt(0)) {
                Left(s"Negative VersionNumberComponent values are not valid: ${value} (parsed as ${bigInt})")
              } else {
                Right(VersionNumberComponentImpl(value, bigInt))
              }
          )
      } else {
        Left(
          s"Invalid VersionNumberComponent value: ${value}. VersionNumberComponent VersionComponent must contain only digits and must not contain leading zeros ${numericNoLeadingZerosRegex}"
        )
      }

    /** As [[#fromString]], but will throw an exception if the value is invalid.
      *
      * It is strongly recommended that you only use this on the REPL or in
      * tests.
      */
    def unsafeFromString(value: String): VersionNumberComponent =
      fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

    /** Create a [[VersionComponent#VersionNumberComponent]] from a `BigInt`.
      * value. The [[java.lang.String]] representation of the component will
      * always be the `toString` of the `BigInt`.
      */
    def fromBigInt(value: BigInt): VersionNumberComponent = VersionNumberComponentImpl(value.toString, value)
  }

  /** A [[VersionComponent]] which represents a pre-release component.
    *
    * [[PreRelease]] components may contain `[0-9A-Za-z-]` characters, but if
    * they are entirely numeric (they match `\d+`), then they must not have
    * any leading zeros.
    *
    * A [[PreRelease]] value of `-0`, is ''not numeric''. Since numeric
    * components are ''not'' permitted to be negative, but '-' is a valid
    * pre-release character, then a [[PreRelease]] component of `-0` would be
    * a ''valid'' alphanumeric (but not strictly numeric) component.
    *
    * This format is formally defined by section 9 of the SemVer
    * specification. Other supported version schemes do not directly have a
    * notion of a pre-release value.
    *
    * @note Some common examples of pre-release components are `SNAPSHOT` in
    *       `1.0.0-SNAPSHOT` or `RC1` in `1.0.0-RC1`. Note that the '-'
    *       character in these examples is ''not'' part of the pre-release
    *       components. The first '-' in a version string is the separator
    *       which starts a pre-release section. For example, in the (unusual)
    *       version string `"1.0.0--RC1"`, there is a single pre-release
    *       component, namely `-RC1`, but ''not'' `--RC1`.
    *
    * @see [[https://semver.org/ semver]]
    */
  sealed abstract class PreRelease extends VersionComponent {
    final override def toString: String = s"PreRelease(value = ${value})"
  }

  object PreRelease {
    final private[this] case class PreReleaseImpl(override val value: String) extends PreRelease

    /** Attempt to create a [[PreRelease]] component from a [[java.lang.String]].
      */
    def fromString(value: String): Either[String, PreRelease] = {
      if (isValidPreReleaseString(value)) {
        Right(PreReleaseImpl(value))
      } else {
        Left(
          s"Failed to parse ${value} as PreRelease VersionComponent. PreRelease VersionComponent's must only contain [0-9A-Za-z-], be non-empty, and if they are entirely numeric they can not start with leading 0s. ${whitespaceWarningString}"
        )
      }
    }

    /** As [[#fromString]], but will throw an exception if the value is invalid.
      *
      * It is strongly recommended that you only use this on the REPL or in
      * tests.
      */
    def unsafeFromString(value: String): PreRelease =
      fromString(value).fold(e => throw new IllegalArgumentException(e), identity)
  }

  /** A [[VersionComponent]] which represents a metadata component.
    *
    * [[Metadata]] components may contain `[0-9A-Za-z-]` characters. They are
    * similar in format to [[PreRelease]] components, but differ in that they
    * ''may'' contain leading zeros. They must be non-empty.
    *
    * They most commonly used to identity a particular build or VCS commit.
    *
    * This format is formally defined by section 10 of the SemVer
    * specification. Other supported version schemes do not directly have a
    * notion of metadata.
    *
    * @note An example metadata value would be `9934a5e` in
    *       "1.0.0-SNAPSHOT+9934a5e"`.
    *
    * @see [[https://semver.org/ semver]]
    */
  sealed abstract class Metadata extends VersionComponent {
    final override def toString: String = s"Metadata(value = ${value})"
  }

  object Metadata {
    final private[this] case class MetadataImpl(override val value: String) extends Metadata

    /** Attempt to create a [[Metadata]] value from a [[java.lang.String]]
      * yielding an error if it is invalid.
      */
    def fromString(value: String): Either[String, Metadata] = {
      if (metadataValidNonNumericRegex.pattern.matcher(value).matches) {
        Right(MetadataImpl(value))
      } else {
        Left(
          s"Failed to parse ${value} as Metadata VersionComponent. Metadata VersionComponent's must only contain [0-9A-Za-z-] and be non-empty. ${whitespaceWarningString}"
        )
      }
    }

    /** As [[#fromString]], but will throw an exception if the value is invalid.
      *
      * It is strongly recommended that you only use this on the REPL or in
      * tests.
      */
    def unsafeFromString(value: String): Metadata =
      fromString(value).fold(e => throw new IllegalArgumentException(e), identity)
  }

  /** Attempt to parse a [[java.lang.String]] representing a version into a
    * [[Vector]] of [[VersionComponent]] values.
    *
    * This method makes the following assumptions,
    *
    * - The version string may contain a numeric section, a pre-release
    *   section, and a metadata section. No section is required. They must
    *   occur in that order.
    * - After parsing all the sections, there should be no remaining
    *   characters.
    *
    * This parsing scheme is based on SemVer's definition, but is compatible
    * with the other supported version schemes. That said, successful parsing
    * here does ''not'' mean that the version string is valid according to
    * some given scheme. The constructors for the various version scheme data
    * types perform the final validation for that.
    *
    * @note For the purposes of this function the version string ''may'' be
    *       empty. This will yield an empty [[Vector]] result.
    *
    * @see [[https://semver.org/ semver]]
    */
  def fromVersionString(value: String): Either[String, Vector[VersionComponent]] = {

    def parseSection(
      value: String,
      constructor: String => Either[String, VersionComponent]
    ): Either[String, Vector[VersionComponent]] =
      if (value.isEmpty) {
        Right(Vector.empty[VersionComponent])
      } else {
        val components: Vector[String] = value.split('.').toVector

        if (components.exists(_.isEmpty)) {
          // If any component is empty, then we have a section of the input
          // string with two or more neighboring . characters,
          // e.g. `1..0`. This makes treating the input as . separated
          // VersionNumberComponent components invalid.
          Left(s"An empty component section exists, but that is not valid: ${value}")
        } else {
          components.foldLeft(Right(Vector.empty[VersionComponent]): Either[String, Vector[VersionComponent]]) {
            case (acc, value) =>
              acc.flatMap(acc => constructor(value).map(value => acc ++ Vector(value)))
          }
        }
      }

    def parseNumeric(value: String): Either[String, Vector[VersionComponent]] =
      parseSection(value, VersionNumberComponent.fromString)

    def parsePreRelease(value: String): Either[String, Vector[VersionComponent]] =
      parseSection(value, PreRelease.fromString)

    def parseMetadata(value: String): Either[String, Vector[VersionComponent]] =
      parseSection(value, Metadata.fromString)

    def validateRest(rest: String, parsed: Vector[VersionComponent]): Either[String, Unit] =
      if (rest.isEmpty) {
        Right(())
      } else {
        Left(
          s"""Successfully able to parse leading VersionComponent values, but there was a non-empty trailing value. This is not permitted. Version String: ${value}, Parsed: ${parsed}, Non Empty Trailing Component: ${rest}, Trailing Size: ${rest
            .size}."""
        )
      }

    val (numeric, preReleaseAndMetadata) = value.trim.span((c: Char) => c.isDigit || c === '.')
    val (preRelease, metaDataAndRest) =
      if (preReleaseAndMetadata.startsWith("-")) {
        preReleaseAndMetadata.drop(1).span((c: Char) => preReleaseAndMetadataValidCharPred(c))
      } else {
        // Pre-release data _must_ start with a '-', thus no pre-release data
        // was found.
        ("", preReleaseAndMetadata)
      }
    val (metaData, rest) =
      if (metaDataAndRest.startsWith("+")) {
        metaDataAndRest.drop(1).span((c: Char) => preReleaseAndMetadataValidCharPred(c))
      } else {
        // Metadata data _must_ start with a '+', thus no metadata was found.
        ("", metaDataAndRest)
      }

    for {
      numericSection    <- parseNumeric(numeric)
      preReleaseSection <- parsePreRelease(preRelease)
      metaDataSection   <- parseMetadata(metaData)
      parsed = numericSection ++ preReleaseSection ++ metaDataSection
      _ <- validateRest(rest, parsed)
    } yield parsed
  }

  /** As [[#fromVersionString]], but throws an exception if the version string
    * is invalid.
    *
    * It is strongly recommended that you only use this on the REPL or in
    * tests.
    */
  def unsafeFromVersionString(value: String): Vector[VersionComponent] =
    fromVersionString(value).fold(e => throw new IllegalArgumentException(e), identity)
}
