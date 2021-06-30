package io.isomarcte.sbt.version.scheme.enforcer.core.vcs

import cats.kernel._
import java.time._
import org.scalacheck._

private[enforcer] trait OrphanTagInstances {

  implicit final lazy val catsInstancesForTag: Hash[Tag] with Order[Tag] = {
    val order: Order[Tag] = Order.fromComparable[Tag]
    new Hash[Tag] with Order[Tag] {
      override def hash(a: Tag): Int = a.hashCode

      override def compare(a: Tag, b: Tag): Int = order.compare(a, b)
    }
  }

  implicit final lazy val arbTag: Arbitrary[Tag] = Arbitrary(
    for {
      value        <- Arbitrary.arbitrary[String]
      creationDate <- Arbitrary.arbitrary[Option[OffsetDateTime]]
    } yield Tag(value, creationDate)
  )

  implicit final lazy val cogenTag: Cogen[Tag] = Cogen[(String, Option[OffsetDateTime])]
    .contramap(value => (value.value, value.creationDate))
}

private[enforcer] object OrphanTagInstances extends OrphanTagInstances
