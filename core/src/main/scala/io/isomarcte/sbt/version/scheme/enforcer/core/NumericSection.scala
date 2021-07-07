package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.Try

/** A data type which models the numeric section of a version string.
  *
  * The numeric section is made up of numeric components, represented by
  * instances of [[NumericVersionToken]]. Each component is '.' separated,
  * must be non-empty, and may only contain digits. Leading zeros are not
  * permitted.
  *
  * These values are what people commonly think of when they think of a
  * "version number".
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
  * }}}
  */
sealed abstract class NumericSection extends Product with Serializable {

  def value: Vector[NumericVersionToken]

  // final //

  /** The representation of the numeric section as derived from the components.
    *
    * If the version string was parsed directly from
    * [[NumericSection#fromString]], this should be the same as the original
    * numeric section input.
    */
  final def canonicalString: String =
    value.map(_.value.toString).mkString(".")

  override final def toString: String =
    s"NumericSection(${canonicalString})"
}

object NumericSection {

  private[this] final case class NumericSectionImpl(override val value: Vector[NumericVersionToken]) extends NumericSection

  private[this] val errorBaseMessage: String =
    "Invalid numeric section. The numeric section of a version string is a series of non-empty, dot (.) separated, sections of only digits. Components may not contain leading zeros."

  private[this] def errorMessage(message: String, input: String): String =
    s"${errorBaseMessage} ${message}: ${input}"

  /** Create a new [[NumericSection]] from components. */
  def apply(value: Vector[NumericVersionToken]): NumericSection =
    NumericSectionImpl(value)

  /** Attempt to create a [[NumericSection]] from a [[java.lang.String]].
    *
    * The string must contain only [0-9.] and may be empty. Leading zeros are
    * not permitted.
    */
  def fromString(value: String): Either[String, NumericSection] =
    if (value.isEmpty) {
      Right(empty)
    } else {
      value.split(internal.separatorRegexString).toVector.foldLeft(Right(Vector.empty): Either[String, Vector[NumericVersionToken]]){
        case (acc, value) =>
          acc.flatMap(acc =>
            Try(BigInt(value)).toEither.fold(
              e => Left(e.getLocalizedMessage): Either[String, NumericVersionToken],
              NumericVersionToken.fromBigInt
            ).map(value =>
              acc ++ Vector(value)
            )
          )
      }.map(NumericSectionImpl.apply _).fold(
        e => Left(errorMessage(s"Error parsing section. $e", value)): Either[String, NumericSection],
        value => Right(value)
      )
    }

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
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

  /** The empty [[NumericSection]].
    *
    * This is same as the result of parsing "".
    */
  val empty: NumericSection = NumericSectionImpl(Vector.empty)
}
