package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.matching.Regex
import org.scalacheck._
import cats.kernel._

private[enforcer] trait OrphanInstances {

  private[this] val genMetadataVersionTokenChar: Gen[Char] = Gen.oneOf(
    (Range('0', '9').toVector ++ Range('A', 'Z').toVector ++ Range('a', 'z').toVector ++ Vector('-'.toInt))
      .map(_.toChar)
  )

  /** The same character set is valid for both metadata and pre-release, though
    * pre-release has additional rules if the first character is a '0'.
    */
  private[this] def genPreReleaseVersionTokenChar: Gen[Char] =
    genMetadataVersionTokenChar

  private[this] val genMetadataVersionTokenString: Gen[String] =
    Gen.nonEmptyListOf(genMetadataVersionTokenChar).map(_.mkString)

  private[this] val numericWithLeadingZerosRegex: Regex =
    """0\d+""".r

  private[this] val genNonNegativeBigInt: Gen[BigInt] =
    Arbitrary.arbitrary[BigInt].map(_.abs)

  private[this] def genPreReleaseVersionTokenString: Gen[String] =
    Gen.frequency(
      (4 -> genNonNegativeBigInt.map(_.toString)),
      (6 ->
        Gen.nonEmptyListOf(genPreReleaseVersionTokenChar).map(_.mkString).map(value =>
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

  implicit final val arbMetadataVersionToken: Arbitrary[MetadataVersionToken] =
    Arbitrary(genMetadataVersionTokenString.map(MetadataVersionToken.unsafeFromString))

  implicit final val cogenMetadataVersionToken: Cogen[MetadataVersionToken] =
    Cogen[String].contramap(_.value)

  implicit final val catsInstancesForMetadataVersionToken: Hash[MetadataVersionToken] with Order[MetadataVersionToken] =
    catsHashAndOrderFromOrdering[MetadataVersionToken]

  implicit final val arbMetadataSection: Arbitrary[MetadataSection] =
    Arbitrary(
      Arbitrary.arbitrary[Vector[MetadataVersionToken]].map(MetadataSection.apply)
    )

  implicit final val cogenMetadataSection: Cogen[MetadataSection] =
    Cogen[Vector[MetadataVersionToken]].contramap(_.value)

  implicit final val catsInstancesForMetadataSection: Hash[MetadataSection] with Order[MetadataSection] =
    catsHashAndOrderFromOrdering[MetadataSection]

  implicit final val arbNumericVersionToken: Arbitrary[NumericVersionToken] =
    Arbitrary(
      genNonNegativeBigInt.map(NumericVersionToken.unsafeFromBigInt)
    )

  implicit final val cogenNumericVersionToken: Cogen[NumericVersionToken] =
    Cogen[BigInt].contramap(_.value)

  implicit final val catsInstancesForNumericVersionToken: Hash[NumericVersionToken] with Order[NumericVersionToken] =
    catsHashAndOrderFromOrdering[NumericVersionToken]

  implicit final val arbNumericSection: Arbitrary[NumericSection] =
    Arbitrary(
      Arbitrary.arbitrary[Vector[NumericVersionToken]].map(NumericSection.apply)
    )

  implicit final val cogenNumericSection: Cogen[NumericSection] =
    Cogen[Vector[NumericVersionToken]].contramap(_.value)

  implicit final val catsInstancesForNumericSection: Hash[NumericSection] with Order[NumericSection] =
    catsHashAndOrderFromOrdering[NumericSection]

  implicit final val arbPreReleaseVersionToken: Arbitrary[PreReleaseVersionToken] =
    Arbitrary(
      genPreReleaseVersionTokenString.map(
        PreReleaseVersionToken.unsafeFromString
      )
    )

  implicit final val cogenPreReleaseVersionToken: Cogen[PreReleaseVersionToken] =
    Cogen[String].contramap(_.value)

  implicit final val catsInstancesForPreReleaseVersionToken: Hash[PreReleaseVersionToken] with Order[PreReleaseVersionToken] =
    catsHashAndOrderFromOrdering[PreReleaseVersionToken]

  implicit final val arbPreReleaseSection: Arbitrary[PreReleaseSection] =
    Arbitrary(
      Arbitrary.arbitrary[Vector[PreReleaseVersionToken]].map(PreReleaseSection.apply)
    )

  implicit final val cogenPreReleaseSection: Cogen[PreReleaseSection] =
    Cogen[Vector[PreReleaseVersionToken]].contramap(_.value)

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
