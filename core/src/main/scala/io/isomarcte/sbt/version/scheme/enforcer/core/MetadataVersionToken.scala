package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.matching.Regex

/** A data type for an individual component of a [[MetadataSection]] of a
  * version string.
  *
  * Within a metadata section of a version string, components are '.'
  * separated values.
  *
  * A metadata component is structurally any string which matches
  * `[0-9A-Za-z-]+`. Individual components may not be empty.
  *
  * For example,
  *
  * {{{
  * scala> MetadataVersionToken.unsafeFromString("00")
  * res0: io.isomarcte.sbt.version.scheme.enforcer.core.MetadataVersionToken = MetadataVersionToken(value = 00)
  *
  * scala> MetadataSection.unsafeFromString("+00.1-2A.abc-")
  * res1: io.isomarcte.sbt.version.scheme.enforcer.core.MetadataSection = MetadataSection(+00.1-2A.abc-)
  *
  * scala> MetadataSection.unsafeFromString("+00.1-2A.abc-").value
  * res2: Vector[io.isomarcte.sbt.version.scheme.enforcer.core.MetadataVersionToken] = Vector(MetadataVersionToken(value = 00), MetadataVersionToken(value = 1-2A), MetadataVersionToken(value = abc-))
  *
  * scala> VersionSections.unsafeFromString("1.0.0-RC1+00.1-2A.abc-").metadataSection
  * res3: Option[io.isomarcte.sbt.version.scheme.enforcer.core.MetadataSection] = Some(MetadataSection(+00.1-2A.abc-))
  *
  * scala> VersionSections.unsafeFromString("1.0.0-RC1+00.1-2A.abc-").metadataSection.map(_.value)
  * res4: Option[Vector[io.isomarcte.sbt.version.scheme.enforcer.core.MetadataVersionToken]] = Some(Vector(MetadataVersionToken(value = 00), MetadataVersionToken(value = 1-2A), MetadataVersionToken(value = abc-)))
  * }}}
  */
sealed abstract class MetadataVersionToken extends Product with Serializable {
  def value: String

  // final //

  override final def toString: String =
    s"MetadataVersionToken(value = ${value})"
}

object MetadataVersionToken {
  private[this] final case class MetadataVersionTokenImpl(override val value: String) extends MetadataVersionToken

  private[this] val metadataComponentRegex: Regex = """^[0-9A-Za-z-]+$""".r

  /** Attempt to create a [[MetadataVersionToken]] from a [[java.lang.String]].
    *
    * The value must match `[0-9A-Za-z-]+`.
    */
  def fromString(value: String): Either[String, MetadataVersionToken] =
    if (metadataComponentRegex.pattern.matcher(value).matches) {
      Right(MetadataVersionTokenImpl(value))
    } else {
      Left(s"Invalid MetadataVersionToken value (must match ${metadataComponentRegex}: ${value})")
    }

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that you only use this on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): MetadataVersionToken =
    fromString(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit val orderingInstance: Ordering[MetadataVersionToken] =
    Ordering.by(_.value)
}
