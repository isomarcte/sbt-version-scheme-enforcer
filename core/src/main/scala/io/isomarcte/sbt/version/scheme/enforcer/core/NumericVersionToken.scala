package io.isomarcte.sbt.version.scheme.enforcer.core

/** A data type for an individual component of a [[NumericSection]] of a
  * version string.
  *
  * Within a numeric section of a version string, components are '.' separated
  * values.
  *
  * A numeric component may is a non-negative integral value.
  *
  * For example,
  *
  * {{{
  * scala> val versionString: String = "1.0.0"
  * versionString: String = 1.0.0
  *
  * scala> VersionSections.unsafeFromString(versionString).numericSection
  * res0: io.isomarcte.sbt.version.scheme.enforcer.core.NumericSection = NumericSection(1.0.0)
  *
  * scala> NumericSection.unsafeFromString(versionString).value
  * res1: Vector[io.isomarcte.sbt.version.scheme.enforcer.core.NumericVersionToken] = Vector(NumericVersionToken(value = 1), NumericVersionToken(value = 0), NumericVersionToken(value = 0))
  *
  * scala> NumericVersionToken.unsafeFromBigInt(BigInt(1))
  * res1: io.isomarcte.sbt.version.scheme.enforcer.core.NumericVersionToken = NumericVersionToken(value = 1)
  * }}}
  */
sealed abstract class NumericVersionToken extends Product with Serializable {
  def value: BigInt

  // final //

  final override def toString: String =
    s"NumericVersionToken(value = ${value})"
}

object NumericVersionToken {
  private[this] final case class NumericVersionTokenImpl(override val value: BigInt) extends NumericVersionToken

  /** Attempt to create [[NumericVersionToken]] from a [[BigInt]] value.
    *
    * The value must be >= 0.
    */
  def fromBigInt(value: BigInt): Either[String, NumericVersionToken] =
    if (value >= BigInt(0)) {
      Right(NumericVersionTokenImpl(value))
    } else {
      Left(s"Invalid NumericVersionToken, must be >= 0: ${value}")
    }

  /** As [[#fromBigInt]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that you only use this on the REPL and in
    * tests.
    */
  def unsafeFromBigInt(value: BigInt): NumericVersionToken =
    fromBigInt(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit lazy val orderingInstance: Ordering[NumericVersionToken] =
    Ordering.by(_.value)
}
