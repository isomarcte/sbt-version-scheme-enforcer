package io.isomarcte.sbt.version.scheme.enforcer.core.vcs

import cats.kernel._
import java.time._
import org.scalacheck._

private[enforcer] trait OrphanTagInstances {

  implicit final def catsInstancesForTag[A: Ordering]: Hash[Tag[A]] with Order[Tag[A]] = {
    val order: Order[Tag[A]] = Order.fromOrdering[Tag[A]]
    new Hash[Tag[A]] with Order[Tag[A]] {
      override def hash(a: Tag[A]): Int = a.hashCode

      override def compare(a: Tag[A], b: Tag[A]): Int = order.compare(a, b)
    }
  }

  implicit final def arbTag[A: Arbitrary]: Arbitrary[Tag[A]] = Arbitrary(
    for {
      version        <- Arbitrary.arbitrary[A]
      creationDate <- Arbitrary.arbitrary[Option[OffsetDateTime]]
    } yield Tag(version, creationDate)
  )

  implicit final def cogenTag[A: Cogen]: Cogen[Tag[A]] = Cogen[(A, Option[OffsetDateTime])]
    .contramap(value => (value.version, value.creationDate))
}

private[enforcer] object OrphanTagInstances extends OrphanTagInstances
