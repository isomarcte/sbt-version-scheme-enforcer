package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.Try
import scala.util.matching.Regex

sealed abstract class NumericSection extends Product with Serializable {

  def value: Vector[NumericVersionToken]

  // final //

  final def canonicalString: String =
    value.map(_.value.toString).mkString(".")

  override final def toString: String =
    s"NumericSection(${canonicalString})"
}

object NumericSection {
  private[this] val numericSectionRegex: Regex = """^[0-9.]*$""".r

  private[this] final case class NumericSectionImpl(override val value: Vector[NumericVersionToken]) extends NumericSection

  val empty: NumericSection = NumericSectionImpl(Vector.empty)

  def apply(value: Vector[NumericVersionToken]): NumericSection =
    NumericSectionImpl(value)

  def fromString(value: String): Either[String, NumericSection] =
    if (numericSectionRegex.pattern.matcher(value).matches) {
      value.split('.').toVector.foldLeft(Right(Vector.empty): Either[String, Vector[NumericVersionToken]]){
        case (acc, value) =>
          acc.flatMap(acc =>
            Try(BigInt(value)).toEither.fold(
              e => Left(e.getLocalizedMessage): Either[String, NumericVersionToken],
              NumericVersionToken.fromBigInt
            ).map(value =>
              acc ++ Vector(value)
            )
          )
      }.map(NumericSectionImpl.apply _)
    } else {
      Left(s"Invalid numeric section. Numeric sections may only contain [0-9.]: ${value}")
    }

  def unsafeFromString(value: String): NumericSection =
    fromString(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit val ordering: Ordering[NumericSection] =
    new Ordering[NumericSection] {
      override def compare(x: NumericSection, y: NumericSection): Int =
        x.canonicalString.compare(y.canonicalString)
    }
}
