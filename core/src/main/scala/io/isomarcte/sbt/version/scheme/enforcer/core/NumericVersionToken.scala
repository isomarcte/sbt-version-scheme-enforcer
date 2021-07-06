package io.isomarcte.sbt.version.scheme.enforcer.core

sealed abstract class NumericVersionToken extends Product with Serializable {
  def value: BigInt

  // final //

  final override def toString: String =
    s"NumericVersionToken(value = ${value})"
}

object NumericVersionToken {
  private[this] final case class NumericVersionTokenImpl(override val value: BigInt) extends NumericVersionToken

  def fromBigInt(value: BigInt): Either[String, NumericVersionToken] =
    if (value >= BigInt(0)) {
      Right(NumericVersionTokenImpl(value))
    } else {
      Left(s"Invalid NumericVersionToken, must be >= 0: ${value}")
    }

  def unsafeFromBigInt(value: BigInt): NumericVersionToken =
    fromBigInt(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit lazy val orderingInstance: Ordering[NumericVersionToken] =
    Ordering.by(_.value)
}
