package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** A data type which represents a common encoding of the components of a
  * version string.
  */
sealed abstract class VersionComponents extends Product with Serializable {
  def numericSection: NumericSection
  def preReleaseSection: PreReleaseSection
  def metadataSection: MetadataSection

  // final //

  final def canonicalString: String =
    s"${numericSection.canonicalString}${preReleaseSection.canonicalString}${metadataSection.canonicalString}"

  override final def toString: String =
    s"VersionComponents(numericSection = ${numericSection}, preReleaseSection = ${preReleaseSection}, metadataSection = ${metadataSection})"
}

object VersionComponents {
  private[this] final case class VersionComponentsImpl(override val numericSection: NumericSection, override val preReleaseSection: PreReleaseSection, override val metadataSection: MetadataSection) extends VersionComponents

  val empty: VersionComponents = VersionComponentsImpl(NumericSection.empty, PreReleaseSection.empty, MetadataSection.empty)

  def apply(numericSection: NumericSection, preReleaseSection: PreReleaseSection, metadataSection: MetadataSection): VersionComponents =
    VersionComponentsImpl(numericSection, preReleaseSection, metadataSection)

  def apply(numericSection: NumericSection, preReleaseSection: PreReleaseSection): VersionComponents =
    VersionComponentsImpl(numericSection, preReleaseSection, MetadataSection.empty)

  def apply(numericSection: NumericSection, metadataSection: MetadataSection): VersionComponents =
    VersionComponentsImpl(numericSection, PreReleaseSection.empty, metadataSection)

  def apply(preReleaseSection: PreReleaseSection, metadataSection: MetadataSection): VersionComponents =
    VersionComponentsImpl(NumericSection.empty, preReleaseSection, metadataSection)

  def apply(numericSection: NumericSection): VersionComponents =
    VersionComponentsImpl(numericSection, PreReleaseSection.empty, MetadataSection.empty)

  def apply(preReleaseSection: PreReleaseSection): VersionComponents =
    VersionComponentsImpl(NumericSection.empty, preReleaseSection, MetadataSection.empty)

  def apply(metadataSection: MetadataSection): VersionComponents =
    VersionComponentsImpl(NumericSection.empty, PreReleaseSection.empty, metadataSection)

  def fromString(value: String): Either[String, VersionComponents] = {
    val (numeric, rest0) = value.span(char => char =!= '-' && char =!= '+')
    val (preRelease, metadata) = rest0.span(_ =!= '+')

    val preReleaseSection: Either[String, PreReleaseSection] =
      if (preRelease.isEmpty) {
        Right(PreReleaseSection.empty)
      } else {
        PreReleaseSection.fromString(preRelease)
      }

    val metadataSection: Either[String, MetadataSection] =
      if (metadata.isEmpty) {
        Right(MetadataSection.empty)
      } else {
        MetadataSection.fromString(metadata)
      }

    for {
      numericSection <- NumericSection.fromString(numeric)
      preRelease <- preReleaseSection
      metadata <- metadataSection
    } yield apply(numericSection, preRelease, metadata)
  }

  def unsafeFromString(value: String): VersionComponents =
    fromString(value).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  implicit val ordering: Ordering[VersionComponents] =
    new Ordering[VersionComponents] {
      override def compare(x: VersionComponents, y: VersionComponents): Int =
        Ordering[NumericSection].compare(x.numericSection, y.numericSection) match {
          case 0 =>
            Ordering[PreReleaseSection].compare(x.preReleaseSection, y.preReleaseSection) match {
              case 0 =>
                Ordering[MetadataSection].compare(x.metadataSection, y.metadataSection)
              case otherwise =>
                otherwise
            }
          case otherwise =>
            otherwise
        }
    }
}
