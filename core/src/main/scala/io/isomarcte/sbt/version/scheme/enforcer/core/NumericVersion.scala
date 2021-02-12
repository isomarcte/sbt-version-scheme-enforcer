package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._

sealed trait NumericVersion {
  def head: BigInt
  def tail: Vector[BigInt]

  final lazy val asVector: Vector[BigInt] = Vector(head) ++ tail

  final lazy val asVersion: Version = Version(versionString)

  final def _1: BigInt = head

  final lazy val _2: Option[BigInt] = tail.headOption

  final lazy val _3: Option[BigInt] = tail.drop(1).headOption

  final lazy val _4: Option[BigInt] = tail.drop(2).headOption

  final lazy val versionString: String = asVector.mkString(".")

  final override def toString: String = s"NumericVersion(${versionString})"
}

object NumericVersion {
  private[this] val emptyErrorString: String = "Version is empty, which is not a valid numeric version"

  final private[this] case class NumericVersionImpl(override val head: BigInt, override val tail: Vector[BigInt])
      extends NumericVersion

  def fromVector(value: Vector[BigInt]): Either[String, NumericVersion] =
    if (value.exists(_ < BigInt(0))) {
      Left(s"NumericVersion should only have non-negative version components: ${value}"): Either[String, NumericVersion]
    } else {
      value
        .headOption
        .fold(Left(emptyErrorString): Either[String, NumericVersion])(head =>
          Right(NumericVersionImpl(head, value.tail))
        )
    }

  def unsafeFromVector(value: Vector[BigInt]): NumericVersion =
    fromVector(value).fold(e => throw new IllegalArgumentException(e), identity)

  def fromString(value: String): Either[String, NumericVersion] = fromCoursierVersion(Version(value))

  def unsafeFromString(value: String): NumericVersion =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  def fromCoursierVersion(value: Version): Either[String, NumericVersion] = {
    val reverseItems: Vector[Version.Item] = value.items.reverse

    reverseItems
      .headOption
      .fold(Left(emptyErrorString): Either[String, Vector[Version.Item]])(last =>
        if (last.isNumber) {
          Right(Vector(last) ++ reverseItems.tail)
        } else {
          Right(reverseItems.tail)
        }
      )
      .map(_.reverse)
      .flatMap((value: Vector[Version.Item]) =>
        value
          .foldLeft(Right(Vector.empty[BigInt]): Either[String, Vector[BigInt]]) { case (acc, value) =>
            val next: Either[String, BigInt] =
              value match {
                case value: Version.Number =>
                  Right(BigInt(value.value))
                case value: Version.BigNumber =>
                  Right(value.value)
                case otherwise =>
                  Left(s"Expected only integral values in the version number, got ${otherwise}")
              }
            val nextVector: Either[String, Vector[BigInt]] = next.map(value => Vector(value))
            acc.flatMap(acc => nextVector.map(nextVector => acc ++ nextVector))
          }
          .flatMap(NumericVersion.fromVector)
      )
  }
}
