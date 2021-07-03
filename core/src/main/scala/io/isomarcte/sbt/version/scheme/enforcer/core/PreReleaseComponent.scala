package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.matching.Regex
import scala.util.Try
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

sealed abstract class PreReleaseComponent extends Product with Serializable {
  def value: String

  def isNumeric: Boolean
}

object PreReleaseComponent {
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

  sealed abstract class NumericPreReleaseComponent extends PreReleaseComponent {
    def numericValue: BigInt

    // final //

    override final def value: String = numericValue.toString

    override final val isNumeric: Boolean = true

    override final def toString: String = s"NumericPreReleaseComponent(numericValue = ${numericValue})"
  }

  object NumericPreReleaseComponent {
    private[PreReleaseComponent] final case class NumericPreReleaseComponentImpl(override val numericValue: BigInt) extends NumericPreReleaseComponent
  }

  sealed abstract class NonNumericPreReleaseComponent extends PreReleaseComponent {
    // final //
    override final val isNumeric: Boolean = false

    override final def toString: String =
      s"NonNumericPreReleaseComponent(value = ${value})"
  }

  object NonNumericPreReleaseComponent {
    private[PreReleaseComponent] final case class NonNumericPreReleaseComponentImpl(override val value: String) extends NonNumericPreReleaseComponent
  }

  def fromBigInt(value: BigInt): Either[String, PreReleaseComponent] =
    if (value >= BigInt(0)) {
      Right(NumericPreReleaseComponent.NumericPreReleaseComponentImpl(value))
    } else {
      Left(s"NumericPreReleaseComponent values must be >= 0: ${value}")
    }

  def fromString(value: String): Either[String, PreReleaseComponent] =
    if(numericRegex.pattern.matcher(value).matches) {
      if (value.startsWith("0") && value =!= "0") {
        Left(s"NumericPreReleaseComponent values may not contain leading zeros: ${value}"): Either[String, PreReleaseComponent]
      } else {
        Try(BigInt(value)).toEither.fold(
          e => Left(e.getLocalizedMessage): Either[String, BigInt],
          value => Right(value)
        ).flatMap(
          fromBigInt
        )
      }
    } else if (preReleaseValidNonNumericRegex.pattern.matcher(value).matches) {
      Right(NonNumericPreReleaseComponent.NonNumericPreReleaseComponentImpl(value))
    } else {
      Left(s"Invalid PreReleaseComponent value (must match ${preReleaseValidNonNumericRegex} and can not contain leading zeros): ${value}")
    }

  implicit val orderingInstance: Ordering[PreReleaseComponent] =
    new Ordering[PreReleaseComponent]{
      override def compare(x: PreReleaseComponent, y: PreReleaseComponent): Int =
        (x, y) match {
          case (x: NumericPreReleaseComponent, y: NumericPreReleaseComponent) =>
            x.numericValue.compare(y.numericValue)
          case (x: NonNumericPreReleaseComponent, y: NonNumericPreReleaseComponent) =>
            x.value.compareTo(y.value)
          case (_: NumericPreReleaseComponent, _: NonNumericPreReleaseComponent) =>
            -1
          case (_: NonNumericPreReleaseComponent, _: NumericPreReleaseComponent) =>
            1
        }
    }
}
