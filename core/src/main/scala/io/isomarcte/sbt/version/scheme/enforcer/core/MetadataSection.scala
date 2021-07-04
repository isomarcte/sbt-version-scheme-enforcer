package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

sealed abstract class MetadataSection extends Product with Serializable {
  def value: Vector[MetadataComponent]

  // final //

  final def canonicalString: String =
    s"""+${value.map(_.value).mkString(".")}"""

  override final def toString: String =
    s"MetadataSection(${canonicalString})"
}

object MetadataSection {
  private[this] final case class MetadataSectionImpl(override val value: Vector[MetadataComponent]) extends MetadataSection

  val empty: MetadataSection = MetadataSectionImpl(Vector.empty)

  def apply(value: Vector[MetadataComponent]): MetadataSection =
    MetadataSectionImpl(value)

  def fromString(value: String): Either[String, MetadataSection] =
    if (value === "+") {
      // Valid, but empty, metadata
      Right(empty)
    } else if (value.startsWith("+")) {
      value.drop(1).split('.').foldLeft(Right(Vector.empty): Either[String, Vector[MetadataComponent]]){
        case (acc, value) =>
          acc.flatMap(acc =>
            MetadataComponent.fromString(value).map(value =>
              acc ++ Vector(value)
            )
          )
      }.map(apply)
    } else {
      Left(s"Invalid metadata section. The metadata section must begin with a + character: ${value}")
    }

  def unsafeFromString(value: String): MetadataSection =
    fromString(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit val orderingInstance: Ordering[MetadataSection] =
    Ordering.by(_.canonicalString)
}
