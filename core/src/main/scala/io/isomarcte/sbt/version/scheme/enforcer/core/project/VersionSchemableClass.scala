package io.isomarcte.sbt.version.scheme.enforcer.core.project

import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.internal._
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.toSortedSet

trait VersionSchemableClass[F[_], A] extends Serializable { self =>
  def scheme(versionScheme: VersionScheme, value: F[A]): Either[String, F[versionScheme.VersionType]]
}

object VersionSchemableClass {
  type VersionChangeTypeClassId[A] = VersionSchemableClass[Id, A]

  def apply[F[_], A](implicit F: VersionSchemableClass[F, A]): VersionSchemableClass[F, A] = F

  implicit val versionInstance: VersionSchemableClass[Id, Version] =
    new VersionSchemableClass[Id, Version] {
      override def scheme(versionScheme: VersionScheme, value: Id[Version]): Either[String,Id[versionScheme.VersionType]] =
        versionScheme.fromVersion(value)
    }

  implicit def vectorInstance[F[_], A](implicit F: VersionSchemableClass[F, A]): VersionSchemableClass[Lambda[A => Vector[F[A]]], A] =
    new VersionSchemableClass[Lambda[A => Vector[F[A]]], A] {
      override def scheme(versionScheme: VersionScheme, value: Vector[F[A]]): Either[String,Vector[F[versionScheme.VersionType]]] =
        value.foldLeft(Right(Vector.empty): Either[String, Vector[F[versionScheme.VersionType]]]){
          case (acc, value) =>
            acc.flatMap(acc =>
              F.scheme(versionScheme, value).map(value =>
                acc ++ Vector(value)
              )
            )
        }
    }

  implicit def sortedSetInstance[F[_], A](implicit F: VersionSchemableClass[F, A], G: Order1[F]): VersionSchemableClass[Lambda[A => SortedSet[F[A]]], A] = {
    new VersionSchemableClass[Lambda[A => SortedSet[F[A]]], A] {
      override def scheme(versionScheme: VersionScheme, value: SortedSet[F[A]]): Either[String,SortedSet[F[versionScheme.VersionType]]] = {
        implicit val versionTypeOrderingInstance: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        implicit val orderingInstance: Ordering[F[versionScheme.VersionType]] = Order1.orderingFromOrder1[F, versionScheme.VersionType]
        vectorInstance[F, A].scheme(versionScheme, value.toVector).map(value => toSortedSet(value))
      }
    }
  }

  final class VersionSchemableClassOps[F[_], A](lhs: F[A])(implicit F: VersionSchemableClass[F, A]) {
    final def scheme(versionScheme: VersionScheme): Either[String, F[versionScheme.VersionType]] =
      F.scheme(versionScheme, lhs)
  }
}
