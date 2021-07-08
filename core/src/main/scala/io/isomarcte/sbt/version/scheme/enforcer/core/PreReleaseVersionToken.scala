package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import scala.util.Try
import scala.util.matching.Regex

/** A data type for an individual component of a [[PreReleaseSection]] of a
  * version string.
  *
  * Within a pre-release section of a version string, components are '.'
  * separated values.
  *
  * A pre-release component is structurally any string which matches
  * `[0-9A-Z-az-]+`, with the additional caveat that if the component is
  * ''entirely'' composed of digits (matches \d+), then it may ''not'' have
  * leading zeros.
  *
  * @note Perhaps counter intuitively, pre-release components starting with a
  *       '-' ''may'' have leading zeros, for example `--001`. This is because
  *       this pre-release ''is not'' numeric, in that it does not match \d+,
  *       it has a leading '-'.
  *
  * For example,
  *
  * {{{
  * scala> val versionString: String = "1.0.0--001+5"
  * versionString: String = 1.0.0--001+5
  *
  * scala> VersionSections.unsafeFromString(versionString).preReleaseSection
  * res0: Option[io.isomarcte.sbt.version.scheme.enforcer.core.PreReleaseSection] = Some(PreReleaseSection(--001))
  *
  * scala> PreReleaseSection.unsafeFromString("-RC.1").value
  * res1: Vector[io.isomarcte.sbt.version.scheme.enforcer.core.PreReleaseVersionToken] = Vector(NonNumericPreReleaseVersionToken(value = RC), NumericPreReleaseVersionToken(numericValue = 1))
  *
  * scala> PreReleaseVersionToken.unsafeFromString("RC")
  * res2: io.isomarcte.sbt.version.scheme.enforcer.core.PreReleaseVersionToken = NonNumericPreReleaseVersionToken(value = RC)
  * }}}
  *
  * A [[PreReleaseVersionToken]] has two distinct constructors,
  * [[NumericPreReleaseVersionToken]] and
  * [[NonNumericPreReleaseVersionToken]].
  */
sealed abstract class PreReleaseVersionToken extends Product with Serializable {
  def value: String

  /** Whether or not this [[PreReleaseVersionToken]] is numeric.
    */
  def isNumeric: Boolean
}

object PreReleaseVersionToken {

  /** A [[Regex]] which matches any [[java.lang.String]] of only digits.
    * 1
    */
  private[this] val numericRegex: Regex = """\d+""".r

  /** A regular expression for matching non-numeric pre-release values according
    * to SemVer. That is, if the component is not entirely numeric (it
    * contains one more letters or - characters), then it must match this
    * pattern.
    *
    * If the component only contains digits, it must match
    * [[#numericNoLeadingZerosRegex]].
    */
  private[this] val preReleaseValidNonNumericRegex: Regex = """^[0-9A-Za-z-]+$""".r

  /** Data constructor for numeric pre-release components. */
  sealed abstract class NumericPreReleaseVersionToken extends PreReleaseVersionToken {

    /** The value of the pre-release component, as a [[BigInt]]. */
    def numericValue: BigInt

    // final //

    final override def value: String = numericValue.toString

    final override val isNumeric: Boolean = true

    final override def toString: String = s"NumericPreReleaseVersionToken(numericValue = ${numericValue})"
  }

  object NumericPreReleaseVersionToken {
    final private[PreReleaseVersionToken] case class NumericPreReleaseVersionTokenImpl(
      override val numericValue: BigInt
    ) extends NumericPreReleaseVersionToken

    /** Attempt to create a [[NumericPreReleaseVersionToken]] from a
      * [[BigInt]]. The value of must be >= 0.
      */
    def fromBigInt(value: BigInt): Either[String, NumericPreReleaseVersionToken] =
      if (value >= BigInt(0)) {
        Right(NumericPreReleaseVersionTokenImpl(value))
      } else {
        Left(s"NumericPreReleaseVersionToken values must be >= 0: ${value}")
      }

    /** Attempt to create [[NumericPreReleaseVersionToken]] from an `Int` value.
      *
      * The value must be >= 0.
      */
    def fromInt(value: Int): Either[String, NumericPreReleaseVersionToken] = fromBigInt(BigInt(value))

    /** Attempt to create [[NumericPreReleaseVersionToken]] from a `Long` value.
      *
      * The value must be >= 0.
      */
    def fromLong(value: Long): Either[String, NumericPreReleaseVersionToken] = fromBigInt(BigInt(value))

    /** As [[#fromInt]], but throws an exception if the value is invalid.
      *
      * It is ''strongly'' recommended that you only use this on the REPL and in
      * tests.
      */
    def unsafeFromInt(value: Int): NumericPreReleaseVersionToken =
      fromInt(value).fold(e => throw new IllegalArgumentException(e), identity)

    /** As [[#fromLong]], but throws an exception if the value is invalid.
      *
      * It is ''strongly'' recommended that you only use this on the REPL and in
      * tests.
      */
    def unsafeFromLong(value: Long): NumericPreReleaseVersionToken =
      fromLong(value).fold(e => throw new IllegalArgumentException(e), identity)

    /** As [[#fromBigInt]], but throws an exception if the value is invalid.
      *
      * It is ''strongly'' recommended that you only use this on the REPL and in
      * tests.
      */
    def unsafeFromBigInt(value: BigInt): NumericPreReleaseVersionToken =
      fromBigInt(value).fold(e => throw new IllegalArgumentException(e), identity)
  }

  /** Data constructor for non-numeric pre-release components. */
  sealed abstract class NonNumericPreReleaseVersionToken extends PreReleaseVersionToken {
    // final //
    final override val isNumeric: Boolean = false

    final override def toString: String = s"NonNumericPreReleaseVersionToken(value = ${value})"
  }

  object NonNumericPreReleaseVersionToken {
    final private[PreReleaseVersionToken] case class NonNumericPreReleaseVersionTokenImpl(override val value: String)
        extends NonNumericPreReleaseVersionToken

    /** Attempt to create a [[NumericPreReleaseVersionToken]] from a
      * [[java.lang.String]]. The value of must only contain [0-9A-Za-z-] and
      * be non-empty. Additionally it must not contain only digits. If it
      * contains only digits, it would be an instance of
      * [[NumericPreReleaseVersionToken]].
      */
    def fromString(value: String): Either[String, NonNumericPreReleaseVersionToken] =
      if (numericRegex.pattern.matcher(value).matches) {
        Left(
          s"Invalid NonNumericPreReleaseVersionToken, but it may be a valid NumericPreReleaseVersionToken. If you wish to parse this just as a general PreReleaseVersionToken (numeric or otherwise), you should call fromString on PreReleaseVersionToken, not on PreReleaseVersionToken.NonNumericPreReleaseVersionToken: ${value}"
        )
      } else if (preReleaseValidNonNumericRegex.pattern.matcher(value).matches) {
        Right(NonNumericPreReleaseVersionToken.NonNumericPreReleaseVersionTokenImpl(value))
      } else {
        Left(
          s"Invalid PreReleaseVersionToken value (must match ${preReleaseValidNonNumericRegex} and can not contain leading zeros): ${value}"
        )
      }

    /** As [[#fromString]], but throws an exception if the value is invalid.
      *
      * It is ''strongly'' recommended that you only use this on the REPL and in
      * tests.
      */
    def unsafeFromString(value: String): NonNumericPreReleaseVersionToken =
      fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

    /** Constant value for the common "SNAPSHOT" pre-release component. */
    val snapshot: NonNumericPreReleaseVersionToken = unsafeFromString("SNAPSHOT")
  }

  /** Attempt to create a [[PreReleaseVersionToken]] from a [[BigInt]]. The
    * value of must be >= 0.
    */
  def fromBigInt(value: BigInt): Either[String, PreReleaseVersionToken] =
    NumericPreReleaseVersionToken.fromBigInt(value).map(identity /* widen */ )

  /** Attempt to create [[PreReleaseVersionToken]] from an `Int` value.
    *
    * The value must be >= 0.
    */
  def fromInt(value: Int): Either[String, PreReleaseVersionToken] = fromBigInt(BigInt(value))

  /** Attempt to create [[PreReleaseVersionToken]] from a `Long` value.
    *
    * The value must be >= 0.
    */
  def fromLong(value: Long): Either[String, PreReleaseVersionToken] = fromBigInt(BigInt(value))

  /** As [[#fromInt]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that you only use this on the REPL and in
    * tests.
    */
  def unsafeFromInt(value: Int): PreReleaseVersionToken =
    fromInt(value).fold(e => throw new IllegalArgumentException(e), identity)

  /** As [[#fromLong]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that you only use this on the REPL and in
    * tests.
    */
  def unsafeFromLong(value: Long): PreReleaseVersionToken =
    fromLong(value).fold(e => throw new IllegalArgumentException(e), identity)

  /** As [[#fromBigInt]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that you only use this on the REPL and in
    * tests.
    */
  def unsafeFromBigInt(value: BigInt): PreReleaseVersionToken =
    fromBigInt(value).fold(e => throw new IllegalArgumentException(e), identity)

  /** Attempt to create a [[PreReleaseVersionToken]] from a
    * [[java.lang.String]]. The value of must only contain [0-9A-Za-z-] and be
    * non-empty. If it contains only digits, it must not have leading zeros.
    */
  def fromString(value: String): Either[String, PreReleaseVersionToken] =
    if (numericRegex.pattern.matcher(value).matches) {
      if (value.startsWith("0") && value =!= "0") {
        Left(s"NumericPreReleaseVersionToken values may not contain leading zeros: ${value}"): Either[
          String,
          PreReleaseVersionToken
        ]
      } else {
        Try(BigInt(value))
          .toEither
          .fold(e => Left(e.getLocalizedMessage): Either[String, BigInt], value => Right(value))
          .flatMap(fromBigInt)
      }
    } else {
      NonNumericPreReleaseVersionToken.fromString(value).map(identity /* widen */ )
    }

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that you only use this on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): PreReleaseVersionToken =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  /** Constant value for the common "SNAPSHOT" pre-release component. */
  def snapshot: PreReleaseVersionToken = NonNumericPreReleaseVersionToken.snapshot

  implicit val orderingInstance: Ordering[PreReleaseVersionToken] =
    new Ordering[PreReleaseVersionToken] {
      override def compare(x: PreReleaseVersionToken, y: PreReleaseVersionToken): Int =
        (x, y) match {
          case (x: NumericPreReleaseVersionToken, y: NumericPreReleaseVersionToken) =>
            x.numericValue.compare(y.numericValue)
          case (x: NonNumericPreReleaseVersionToken, y: NonNumericPreReleaseVersionToken) =>
            x.value.compareTo(y.value)
          case (_: NumericPreReleaseVersionToken, _: NonNumericPreReleaseVersionToken) =>
            -1
          case (_: NonNumericPreReleaseVersionToken, _: NumericPreReleaseVersionToken) =>
            1
        }
    }
}
