package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.matching.Regex
import scala.util.Try
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

sealed abstract class PreReleaseVersionToken extends Product with Serializable {
  def value: String

  def isNumeric: Boolean
}

object PreReleaseVersionToken {
  /** A [[Regex]] which matches any [[java.lang.String]] of only digits.
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

  sealed abstract class NumericPreReleaseVersionToken extends PreReleaseVersionToken {
    def numericValue: BigInt

    // final //

    override final def value: String = numericValue.toString

    override final val isNumeric: Boolean = true

    override final def toString: String = s"NumericPreReleaseVersionToken(numericValue = ${numericValue})"
  }

  object NumericPreReleaseVersionToken {
    private[PreReleaseVersionToken] final case class NumericPreReleaseVersionTokenImpl(override val numericValue: BigInt) extends NumericPreReleaseVersionToken

    def fromBigInt(value: BigInt): Either[String, NumericPreReleaseVersionToken] =
      if (value >= BigInt(0)) {
        Right(NumericPreReleaseVersionTokenImpl(value))
      } else {
        Left(s"NumericPreReleaseVersionToken values must be >= 0: ${value}")
      }

    def unsafeFromBigInt(value: BigInt): NumericPreReleaseVersionToken =
      fromBigInt(value).fold(
        e => throw new IllegalArgumentException(e),
        identity
      )
  }

  sealed abstract class NonNumericPreReleaseVersionToken extends PreReleaseVersionToken {
    // final //
    override final val isNumeric: Boolean = false

    override final def toString: String =
      s"NonNumericPreReleaseVersionToken(value = ${value})"
  }

  object NonNumericPreReleaseVersionToken {
    private[PreReleaseVersionToken] final case class NonNumericPreReleaseVersionTokenImpl(override val value: String) extends NonNumericPreReleaseVersionToken

    def fromString(value: String): Either[String, NonNumericPreReleaseVersionToken] =
      if(numericRegex.pattern.matcher(value).matches) {
        Left(s"Invalid NonNumericPreReleaseVersionToken, but it may be a valid NumericPreReleaseVersionToken. If you wish to parse this just as a general PreReleaseVersionToken (numeric or otherwise), you should call fromString on PreReleaseVersionToken, not on PreReleaseVersionToken.NonNumericPreReleaseVersionToken: ${value}")
      } else if (preReleaseValidNonNumericRegex.pattern.matcher(value).matches) {
        Right(NonNumericPreReleaseVersionToken.NonNumericPreReleaseVersionTokenImpl(value))
      } else {
        Left(s"Invalid PreReleaseVersionToken value (must match ${preReleaseValidNonNumericRegex} and can not contain leading zeros): ${value}")
      }

    def unsafeFromString(value: String): PreReleaseVersionToken =
      fromString(value).fold(
        e => throw new IllegalArgumentException(e),
        identity
      )
  }

  def fromBigInt(value: BigInt): Either[String, PreReleaseVersionToken] =
    NumericPreReleaseVersionToken.fromBigInt(value).map(identity /* widen */)

  def unsafeFromBigInt(value: BigInt): PreReleaseVersionToken =
    fromBigInt(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  def fromString(value: String): Either[String, PreReleaseVersionToken] =
    if(numericRegex.pattern.matcher(value).matches) {
      if (value.startsWith("0") && value =!= "0") {
        Left(s"NumericPreReleaseVersionToken values may not contain leading zeros: ${value}"): Either[String, PreReleaseVersionToken]
      } else {
        Try(BigInt(value)).toEither.fold(
          e => Left(e.getLocalizedMessage): Either[String, BigInt],
          value => Right(value)
        ).flatMap(
          fromBigInt
        )
      }
    } else {
      NonNumericPreReleaseVersionToken.fromString(value).map(identity /* widen */)
    }

  def unsafeFromString(value: String): PreReleaseVersionToken =
    fromString(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit val orderingInstance: Ordering[PreReleaseVersionToken] =
    new Ordering[PreReleaseVersionToken]{
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
