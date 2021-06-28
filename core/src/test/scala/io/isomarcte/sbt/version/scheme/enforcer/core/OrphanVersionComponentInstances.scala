package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.kernel._
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import io.isomarcte.sbt.version.scheme.enforcer.core.scalacheck.Generators
import org.scalacheck._

private[enforcer] trait OrphanVersionComponentInstances {

  private[this] val validPreReleaseAndMetadataCharGen: Gen[Char] = Gen.oneOf(
    (Range('0', '9').toVector ++ Range('A', 'Z').toVector ++ Range('a', 'z').toVector ++ Vector('-'.toInt))
      .map(_.toChar)
  )

  private[this] val validPreReleaseAndMetadataStringGen: Gen[String] = Gen
    .nonEmptyListOf(validPreReleaseAndMetadataCharGen)
    .map(_.mkString)

  implicit final val catsInstancesForVersionComponent: Hash[VersionComponent] with Order[VersionComponent] = {
    val order: Order[VersionComponent] = Order.fromComparable[VersionComponent]

    new Hash[VersionComponent] with Order[VersionComponent] {
      override def hash(x: VersionComponent): Int = x.hashCode

      override def compare(x: VersionComponent, y: VersionComponent): Int = order.compare(x, y)
    }
  }

  final val genVersionNumberComponent: Gen[VersionComponent.VersionNumberComponent] = Generators
    .nonNegativeBigInt
    .map(VersionComponent.VersionNumberComponent.fromBigInt)

  final val genPreRelease: Gen[VersionComponent.PreRelease] = validPreReleaseAndMetadataStringGen
    .suchThat((value: String) =>
      if (value.startsWith("0")) {
        if ((value === "0") || (value.exists(char => char.isLetter || char === '-'))) {
          true
        } else {
          // Entirely numeric, but has leading zeros, this is illegal.
          false
        }
      } else {
        true
      }
    )
    .map(VersionComponent.PreRelease.unsafeFromString)

  final val genMetadata: Gen[VersionComponent.Metadata] = validPreReleaseAndMetadataStringGen
    .map(VersionComponent.Metadata.unsafeFromString)

  implicit final val arbVersionComponent: Arbitrary[VersionComponent] = Arbitrary(
    Gen.oneOf(genVersionNumberComponent.map(identity), genPreRelease.map(identity), genMetadata.map(identity))
  )

  implicit final val cogenVersionComponent: Cogen[VersionComponent] = Cogen[String].contramap(_.value)
}

private[enforcer] object OrphanVersionComponentInstances extends OrphanVersionComponentInstances
