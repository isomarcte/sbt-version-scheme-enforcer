package io.isomarcte.sbt.version.scheme.enforcer.core

sealed abstract class NumericComponent extends Product with Serializable {
  def value: BigInt

  // final //

  final override def toString: String =
    s"NumericComponent(value = ${value})"
}

object NumericComponent {
  private[this] final case class NumericComponentImpl(override val value: BigInt) extends NumericComponent

  def fromBigInt(value: BigInt): Either[String, NumericComponent] =
    if (value >= BigInt(0)) {
      Right(NumericComponentImpl(value))
    } else {
      Left(s"Invalid NumericComponent, must be >= 0: ${value}")
    }

  def unsafeFromBigInt(value: BigInt): NumericComponent =
    fromBigInt(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit lazy val orderingInstance: Ordering[NumericComponent] =
    Ordering.by(_.value)
}
