package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._

/** A representation of a version with only numeric components.
  *
  * @note This is ''not'' a general purpose version representation. It is
  *       specifically tuned to the needs of calculating changes between
  *       versions as they pertain to a supported versioning scheme.
  */
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

  /** Create a [[NumericVersion]] from a [[scala.collections.Vector]] of [[scala.math.BigInt]] value.
    *
    * @note This will yield a `Left` value if any of the members are < 0 or if
    *       the [[scala.collections.Vector]] is empty.
    */
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

  /** As [[#fromVector]], but throws on invalid input. Should not be used
    * outside of toy code and the REPL.
    */
  def unsafeFromVector(value: Vector[BigInt]): NumericVersion =
    fromVector(value).fold(e => throw new IllegalArgumentException(e), identity)

  /** Create a [[NumericVersion]] from a [[java.lang.String]]. */
  def fromString(value: String): Either[String, NumericVersion] = fromCoursierVersion(Version(value))

  /** As [[#fromString]], but throws on invalid input. Should not be used
    * outside toy code and the REPL.
    */
  def unsafeFromString(value: String): NumericVersion =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  /** Create a [[NumericVersion]] from a [[coursier.version.Version]].
    *
    * The [[coursier.version.Version]] may have a tag at the end,
    * e.g. "1.0.0-SNAPSHOT", which will be removed.
    */
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

  def unsafeFromCoursierVersion(value: Version): NumericVersion =
    fromCoursierVersion(value).fold(e => throw new IllegalArgumentException(e), identity)
}
