package io.isomarcte.sbt.version.scheme.enforcer.core

sealed abstract class PVPVersion extends Ordered[PVPVersion]{
  def value: String

  def versionNumbers: Vector[BigInt]

  // Final //

  final def versionNumberAtIndex(index: Int): Option[BigInt] =
    versionNumbers.drop(index).headOption

  final def a: Option[BigInt] =
    versionNumberAtIndex(0)

  final def b: Option[BigInt] =
    versionNumberAtIndex(1)

  final def c: Option[BigInt] =
    versionNumberAtIndex(2)

  final def rest: Vector[BigInt] =
    versionNumbers.drop(3)

  override final def compare(that: PVPVersion): Int =
    value.compare(that.value)

  override final def toString: String =
    s"PVPVersion(value = ${value})"
}

object PVPVersion {
  private[this] final case class PVPVersionImpl(override val value: String, override val versionNumbers: Vector[BigInt]) extends PVPVersion

  private[this] def isValidVersionValue(value: BigInt): Boolean =
    value >= BigInt(0)

  def fromVersionNumbers(value: Vector[BigInt]): Either[String, PVPVersion] =
    if (value.forall(isValidVersionValue)) {
      Right(PVPVersionImpl(value.mkString("."), versionNumbers = value))
    } else {
      Left(s"One or more of the version components is negative. This is not permitted in PVP versioning: ${value}")
    }

  def fromString(value: String): Either[String, PVPVersion] =
    VersionComponent.fromVersionString(value).foldLeft(Right(Vector.empty[VersionComponent.NonNegativeIntegral]): Either[String, Vector[VersionComponent.NonNegativeIntegral]]){
      case (acc, value: VersionComponent.NonNegativeIntegral) =>
        acc.map(acc =>
          acc ++ Vector(value)
        )
      case (acc, value) =>
        acc.flatMap(Function.const(Left(s"Expected NonNegativeIntegral (Numeric) value, got ${value}. Only NonNegativeIntegral (Numeric) values can be used to construct a PVPVersion. PVPVersion values do not support the notion of pre-release annotations or metadata. You should use PVPVersionWithMetaData for that purpose.")))
    }.map((components: Vector[VersionComponent.NonNegativeIntegral]) =>
      PVPVersionImpl(value, components.map(_.asBigInt))
    )
}
