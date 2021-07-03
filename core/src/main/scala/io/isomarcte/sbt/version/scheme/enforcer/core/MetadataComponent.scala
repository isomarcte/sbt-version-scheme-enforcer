package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.matching.Regex

sealed abstract class MetadataComponent extends Product with Serializable {
  def value: String

  // final //

  override final def toString: String =
    s"MetadataComponent(value = ${value})"
}

object MetadataComponent {
  private[this] final case class MetadataComponentImpl(override val value: String) extends MetadataComponent

  private[this] val metadataComponentRegex: Regex = """^[0-9A-Za-z-]+$""".r

  def fromString(value: String): Either[String, MetadataComponent] =
    if (metadataComponentRegex.pattern.matcher(value).matches) {
      Right(MetadataComponentImpl(value))
    } else {
      Left(s"Invalid MetadataComponent value (must match ${metadataComponentRegex}: ${value})")
    }

  def unsafeFromString(value: String): MetadataComponent =
    fromString(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit val orderingInstance: Ordering[MetadataComponent] =
    Ordering.by(_.value)
}
