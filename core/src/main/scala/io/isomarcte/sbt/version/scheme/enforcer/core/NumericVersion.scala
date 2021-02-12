package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.data._
import cats.syntax.all._
import coursier.version._

sealed trait NumericVersion {
  def value: NonEmptyChain[BigInt]

  lazy val asVersion: Version = Version(versionString)

  lazy val _1: BigInt = value.head

  lazy val _2: Option[BigInt] = value.toChain.toList.drop(1).headOption

  lazy val _3: Option[BigInt] = value.toChain.toList.drop(2).headOption

  lazy val _4: Option[BigInt] = value.toChain.toList.drop(3).headOption

  lazy val versionString: String = value.mkString_(".")

  final override def toString: String = s"NumericVersion(${value})"
}

object NumericVersion {
  private[this] val emptyErrorString: String = "Version is empty, which is not a valid numeric version"

  final private[this] case class NumericVersionImpl(override val value: NonEmptyChain[BigInt]) extends NumericVersion

  def fromChain(value: NonEmptyChain[BigInt]): Either[String, NumericVersion] =
    if (value.exists(_ < BigInt(0))) {
      Left(s"NumericVersion should only have non-negative version components: ${value}"): Either[String, NumericVersion]
    } else {
      Right(NumericVersionImpl(value))
    }

  def unsafeFromChain(value: NonEmptyChain[BigInt]): NumericVersion =
    fromChain(value).fold(e => throw new IllegalArgumentException(e), identity)

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
          .foldLeft(OptionT.apply[Either[String, *], NonEmptyChain[BigInt]](Right(None))) { case (acc, value) =>
            val next: Either[String, BigInt] =
              value match {
                case value: Version.Number =>
                  Right(BigInt(value.value))
                case value: Version.BigNumber =>
                  Right(value.value)
                case otherwise =>
                  Left(s"Expected only integral values in the version number, got ${otherwise}")
              }
            val nextChain: Either[String, NonEmptyChain[BigInt]] = next.map(value => NonEmptyChain.one(value))
            acc.semiflatMap(acc => nextChain.map(nextChain => acc ++ nextChain)).orElseF(nextChain.map(_.pure[Option]))
          }
          .foldF(Left(emptyErrorString): Either[String, NumericVersion])(value => NumericVersion.fromChain(value))
      )
  }
}
