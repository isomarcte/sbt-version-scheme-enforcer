package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.matching.Regex
import org.scalacheck._
import cats.kernel._

private[enforcer] trait OrphanInstances {

  private[this] val genMetadataComponentChar: Gen[Char] = Gen.oneOf(
    (Range('0', '9').toVector ++ Range('A', 'Z').toVector ++ Range('a', 'z').toVector ++ Vector('-'.toInt))
      .map(_.toChar)
  )

  /** The same character set is valid for both metadata and pre-release, though
    * pre-release has additional rules if the first character is a '0'.
    */
  private[this] def genPreReleaseComponentChar: Gen[Char] =
    genMetadataComponentChar

  private[this] val genMetadataComponentString: Gen[String] =
    Gen.nonEmptyListOf(genMetadataComponentChar).map(_.mkString)

  private[this] val numericWithLeadingZerosRegex: Regex =
    """0\d+""".r

  private[this] val genNonNegativeBigInt: Gen[BigInt] =
    Arbitrary.arbitrary[BigInt].map(_.abs)

  private[this] def genPreReleaseComponentString: Gen[String] =
    Gen.frequency(
      (4 -> genNonNegativeBigInt.map(_.toString)),
      (6 ->
        Gen.nonEmptyListOf(genPreReleaseComponentChar).map(_.mkString).map(value =>
          if (numericWithLeadingZerosRegex.pattern.matcher(value).matches) {
            value.last.toString
          } else {
            value
          }
        )
      )
    )

  private[this] def catsHashAndOrderFromOrdering[A](implicit A: Ordering[A]): Hash[A] with Order[A] =
    new Hash[A] with Order[A] {
      override def hash(x: A): Int = x.hashCode

      override def compare(x: A, y: A): Int =
        A.compare(x, y)
    }

  implicit final val arbMetadataComponent: Arbitrary[MetadataComponent] =
    Arbitrary(genMetadataComponentString.map(MetadataComponent.unsafeFromString))

  implicit final val cogenMetadataComponent: Cogen[MetadataComponent] =
    Cogen[String].contramap(_.value)

  implicit final val catsInstancesForMetadataComponent: Hash[MetadataComponent] with Order[MetadataComponent] =
    catsHashAndOrderFromOrdering[MetadataComponent]

  implicit final val arbMetadataSection: Arbitrary[MetadataSection] =
    Arbitrary(
      Arbitrary.arbitrary[Vector[MetadataComponent]].map(MetadataSection.apply)
    )

  implicit final val cogenMetadataSection: Cogen[MetadataSection] =
    Cogen[Vector[MetadataComponent]].contramap(_.value)

  implicit final val catsInstancesForMetadataSection: Hash[MetadataSection] with Order[MetadataSection] =
    catsHashAndOrderFromOrdering[MetadataSection]

  implicit final val arbNumericComponent: Arbitrary[NumericComponent] =
    Arbitrary(
      genNonNegativeBigInt.map(NumericComponent.unsafeFromBigInt)
    )

  implicit final val cogenNumericComponent: Cogen[NumericComponent] =
    Cogen[BigInt].contramap(_.value)

  implicit final val catsInstancesForNumericComponent: Hash[NumericComponent] with Order[NumericComponent] =
    catsHashAndOrderFromOrdering[NumericComponent]

  implicit final val arbNumericSection: Arbitrary[NumericSection] =
    Arbitrary(
      Arbitrary.arbitrary[Vector[NumericComponent]].map(NumericSection.apply)
    )

  implicit final val cogenNumericSection: Cogen[NumericSection] =
    Cogen[Vector[NumericComponent]].contramap(_.value)

  implicit final val catsInstancesForNumericSection: Hash[NumericSection] with Order[NumericSection] =
    catsHashAndOrderFromOrdering[NumericSection]

  implicit final val arbPreReleaseComponent: Arbitrary[PreReleaseComponent] =
    Arbitrary(
      genPreReleaseComponentString.map(
        PreReleaseComponent.unsafeFromString
      )
    )

  implicit final val cogenPreReleaseComponent: Cogen[PreReleaseComponent] =
    Cogen[String].contramap(_.value)

  implicit final val catsInstancesForPreReleaseComponent: Hash[PreReleaseComponent] with Order[PreReleaseComponent] =
    catsHashAndOrderFromOrdering[PreReleaseComponent]

  implicit final val arbPreReleaseSection: Arbitrary[PreReleaseSection] =
    Arbitrary(
      Arbitrary.arbitrary[Vector[PreReleaseComponent]].map(PreReleaseSection.apply)
    )

  implicit final val cogenPreReleaseSection: Cogen[PreReleaseSection] =
    Cogen[Vector[PreReleaseComponent]].contramap(_.value)

  implicit final val catsInstancesForPreReleaseSection: Hash[PreReleaseSection] with Order[PreReleaseSection] =
    catsHashAndOrderFromOrdering[PreReleaseSection]

  implicit final val arbVersionSections: Arbitrary[VersionSections] =
    Arbitrary(
      for {
        numericSection <- Arbitrary.arbitrary[NumericSection]
        preReleaseSection <- Arbitrary.arbitrary[Option[PreReleaseSection]]
        metadataSection <- Arbitrary.arbitrary[Option[MetadataSection]]
      } yield VersionSections(numericSection, preReleaseSection, metadataSection)
    )

  implicit final val cogenVersionSections: Cogen[VersionSections] =
    Cogen[(NumericSection, Option[PreReleaseSection], Option[MetadataSection])].contramap(value =>
      (value.numericSection, value.preReleaseSection, value.metadataSection)
    )

  implicit final val catsInstancesForVersionSections: Hash[VersionSections] with Order[VersionSections] =
    catsHashAndOrderFromOrdering[VersionSections]
}

object OrphanInstances extends OrphanInstances
