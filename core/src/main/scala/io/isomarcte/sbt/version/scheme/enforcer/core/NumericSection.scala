package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.Try
import scala.util.matching.Regex

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

  /** This is dense, let's break it down.
    *
    * - It is anchored a the start and of the input: ^...$
    * - There is a top level branch, denoted by a '|': ^...|...$
    * - There are two outer capturing groups: ^(...)(...)|$
    * - The first capturing group on the first branch can be 0 or 1-9 followed
    *   by any number of other digits, this prevents leading 0s:
    *   (0|[1-9][0-9]*)
    * - The second capturing group on the first branch requires a leading '.',
    *   then permits either a single 0 or 1-9 followed by any number of other
    *   digits. This checks for the section separator, then again checks for a
    *   number which does not contain leading 0s. This group may repeat 0 or
    *   more times.: (\.(0|[1-9][0-9]*))*
    * - Finally the second branch merely permits the empty string: ^...|$
    *
    * This may not have been a helpful explanation...sorry.
    */
  private[this] val numericSectionRegex: Regex = """^((0|[1-9][0-9]*)(\.(0|[1-9][0-9]*))*|)$""".r

  private[this] final case class NumericSectionImpl(override val value: Vector[NumericVersionToken]) extends NumericSection

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
    } else if (numericSectionRegex.pattern.matcher(value).matches) {
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
      }.map(NumericSectionImpl.apply _)
    } else {
      Left(s"""Invalid numeric section. Numeric sections must match ${numericSectionRegex}, or in other words they must contain only digits separated by '.' characters and may not have leading zeros. : ${value}""")
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

  val empty: NumericSection = NumericSectionImpl(Vector.empty)
}
