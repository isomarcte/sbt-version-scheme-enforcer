package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version.{Version => CVersion}

/** A representation of a version with only numeric components.
  *
  * @note This is ''not'' a general purpose version representation. It is
  *       specifically tuned to the needs of calculating changes between
  *       versions as they pertain to a supported versioning scheme.
  */
sealed trait NumericVersion {
  def head: BigInt
  def tail: Vector[BigInt]

  /** Whether or not this version had a tag. */
  def hasTag: Boolean

  final lazy val asVector: Vector[BigInt] = Vector(head) ++ tail

  final lazy val asVersion: CVersion = CVersion(versionString)

  final def _1: BigInt = head

  final lazy val _2: Option[BigInt] = tail.headOption

  final lazy val _3: Option[BigInt] = tail.drop(1).headOption

  final lazy val _4: Option[BigInt] = tail.drop(2).headOption

  final lazy val versionString: String = asVector.mkString(".")

  final override def toString: String = s"NumericVersion(${versionString})"
}

object NumericVersion {
  private[this] val emptyErrorString: String = "Version is empty, which is not a valid numeric version"

  final private[this] case class NumericVersionImpl(
    override val head: BigInt,
    override val tail: Vector[BigInt],
    override val hasTag: Boolean
  ) extends NumericVersion

  /** Normalize a version [[java.lang.String]] value.
    *
    * Often version strings have a format which is intended for humans making
    * them differ from a strict numeric version string, but they will still
    * represent an unambiguous numeric version. The most common example of
    * this is when a version string is prefixed with a 'v' character,
    * e.g. `v1.0.0`. This function attempts to transform the given version
    * [[java.lang.String]] to a format that can be parsed as a
    * [[NumericVersion]].
    *
    * This function is very simple, so parsing may still fail. For example, if
    * the input was "vvvv", this function would transform it to "vvv", which
    * is still not a valid [[NumericVersion]].
    *
    * This function is always invoked when creating a [[NumericVersion]] from
    * any of the [[java.lang.String]] based functions.
    */
  def normalizeVersionString(value: String): String =
    if (value.startsWith("v")) {
      value.drop(1)
    } else {
      value
    }

  /** Create a [[NumericVersion]] from a [[scala.collections.Vector]] of [[scala.math.BigInt]] value.
    *
    * @note This will yield a `Left` value if any of the members are < 0 or if
    *       the [[scala.collections.Vector]] is empty.
    */
  def fromVector(value: Vector[BigInt]): Either[String, NumericVersion] = fromVectorIsTag(value, false)

  /** As [[#fromVector]] but allows you to specify if this is a tagged version, e.g. a RC.
    */
  def fromVectorIsTag(value: Vector[BigInt], isTag: Boolean): Either[String, NumericVersion] =
    if (value.exists(_ < BigInt(0))) {
      Left(s"NumericVersion should only have non-negative version components: ${value}"): Either[String, NumericVersion]
    } else {
      value
        .headOption
        .fold(Left(emptyErrorString): Either[String, NumericVersion])(head =>
          Right(NumericVersionImpl(head, value.tail, isTag))
        )
    }

  /** As [[#fromVector]], but throws on invalid input. Should not be used
    * outside of toy code and the REPL.
    */
  def unsafeFromVector(value: Vector[BigInt]): NumericVersion = unsafeFromVectorIsTag(value, false)

  /** As [[#unsafeFromVector]], but allows declaring if this version is tagged. */
  def unsafeFromVectorIsTag(value: Vector[BigInt], isTag: Boolean): NumericVersion =
    fromVectorIsTag(value, isTag).fold(e => throw new IllegalArgumentException(e), identity)

  /** Create a [[NumericVersion]] from a [[java.lang.String]].
    *
    * If the given [[java.lang.String]] begins with a "v", it will be dropped.
    */
  def fromString(value: String): Either[String, NumericVersion] =
    fromCoursierVersion(CVersion(normalizeVersionString(value)))

  /** As [[#fromString]], but the left projection is a [[java.lang.Throwable]],
    * rather than an error [[java.lang.String]].
    */
  def fromStringT(value: String): Either[Throwable, NumericVersion] =
    fromString(value).fold(errorString => Left(new IllegalArgumentException(errorString)), value => Right(value))

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
  def fromCoursierVersion(value: CVersion): Either[String, NumericVersion] = {
    value
      .items
      .takeWhile {
        case _: CVersion.Number | _: CVersion.BigNumber =>
          true
        case _ =>
          false
      }
      .foldLeft(Right(Vector.empty[BigInt]): Either[String, Vector[BigInt]]) {
        case (acc, value: CVersion.Number) =>
          acc.map(_ ++ Vector(BigInt(value.value)))
        case (acc, value: CVersion.BigNumber) =>
          acc.map(_ ++ Vector(value.value))
        case (acc, otherwise) =>
          acc.flatMap(
            Function.const(
              Left(
                s"Found non-numeric value ${otherwise} after we've filtered them all out. This is a bug in sbt-version-scheme-enforcer-core, please report it."
              )
            )
          )
      }
      .flatMap { (numeric: Vector[BigInt]) =>
        val rest: Vector[CVersion.Item] = value
          .items
          .dropWhile {
            case _: CVersion.Number | _: CVersion.BigNumber =>
              true
            case _ =>
              false
          }
        fromVectorIsTag(numeric, rest.headOption.isDefined)
      }
  }

  def unsafeFromCoursierVersion(value: CVersion): NumericVersion =
    fromCoursierVersion(value).fold(e => throw new IllegalArgumentException(e), identity)
}
