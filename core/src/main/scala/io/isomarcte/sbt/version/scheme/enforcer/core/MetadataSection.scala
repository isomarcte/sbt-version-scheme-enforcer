package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** A data type which models the metadata section of a version string.
  *
  * The metadata section is made up of metadata components, represented by
  * instances of [[MetadataVersionToken]]. A metadata section of a version
  * string always begins with a '+' character and then continues as long as
  * each character matches [0-9A-Za-z-.]. Each component is '.' separated and
  * the leading '+' is not part of any component.
  *
  * It has no formal meaning, but is typically used to denote things like
  * build date or the commit hash.
  *
  * For example,
  *
  * {{{
  * scala> val version: String = "1.0.0-RC1+2021-07-04.d989857"
  * version: String = 1.0.0-RC1+2021-07-04.d989857
  *
  * scala> VersionSections.unsafeFromString(version).metadataSection
  * res0: Option[io.isomarcte.sbt.version.scheme.enforcer.core.MetadataSection] = Some(MetadataSection(+2021-07-04.d989857))
  *
  * scala> MetadataSection.unsafeFromString("+2021-07-04.d989857").value
  * res1: Vector[io.isomarcte.sbt.version.scheme.enforcer.core.MetadataVersionToken] = Vector(MetadataVersionToken(value = 2021-07-04), MetadataVersionToken(value = d989857))
  * }}}
  *
  * @note A [[MetadataSection]] may have no components. This can occur when
  *       parsing the string "+". "" is not valid.
  */
sealed abstract class MetadataSection extends Product with Serializable {

  def value: Vector[MetadataVersionToken]

  // final //

  /** The representation of the metadata as derived from the components.
    *
    * If the version string was parsed directly from
    * [[MetadataSection#fromString]], this should be the same as the
    * original metadata section input.
    */
  final def canonicalString: String =
    s"""+${value.map(_.value).mkString(".")}"""

  override final def toString: String =
    s"MetadataSection(${canonicalString})"
}

object MetadataSection {
  private[this] final case class MetadataSectionImpl(override val value: Vector[MetadataVersionToken]) extends MetadataSection

  private[this] val errorBaseMessage: String =
    "Invalid metadata section. A metadata section of a version string begins with a + character and is followed by a series of non-empty, dot (.) separated, strings containing only the characters [0-9A-Za-z-] (alphanumeric characters and -)."

  private[this] def errorMessage(message: String, input: String): String =
    s"${errorBaseMessage} ${message}: ${input}"

  /** Create a new [[MetadataSection]] from components. */
  def apply(value: Vector[MetadataVersionToken]): MetadataSection =
    MetadataSectionImpl(value)

  /** Attempt to create a [[MetadataSection]] from a [[java.lang.String]].
    *
    * The string must being with a '+' and can only contain [0-9A-Za-z-.].
    */
  def fromString(value: String): Either[String, MetadataSection] =
    if (value === "+") {
      // Valid, but empty, metadata
      Right(empty)
    } else if (value.startsWith("+")) {
      value.drop(1).split(internal.separatorRegexString).foldLeft(Right(Vector.empty): Either[String, Vector[MetadataVersionToken]]){
        case (acc, value) =>
          acc.flatMap(acc =>
            MetadataVersionToken.fromString(value).map(value =>
              acc ++ Vector(value)
            )
          )
      }.fold(
        e => Left(errorMessage(s"Error parsing section. $e", value)),
        value => Right(apply(value))
      )
    } else {
      Left(errorMessage("Missing leading + character.", value))
    }

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): MetadataSection =
    fromString(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  /** The empty [[MetadataSection]].
    *
    * This is same as the result of parsing "+".
    */
  val empty: MetadataSection = MetadataSectionImpl(Vector.empty)

  implicit val orderingInstance: Ordering[MetadataSection] =
    Ordering.by(_.canonicalString)
}
