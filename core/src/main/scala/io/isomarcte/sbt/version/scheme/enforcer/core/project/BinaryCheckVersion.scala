package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import io.isomarcte.sbt.version.scheme.enforcer.core._

sealed abstract class BinaryCheckVersion[A] extends Product with Serializable {
  def asEither: Either[Tag[A], A]

  // final //

  final def underlyingVersion: A = asEither.fold(_.version, identity)
  final def isTag: Boolean = asEither.isLeft
  final def asTag: Option[Tag[A]] = asEither.fold(tag => Some(tag), Function.const(None))
  final def fold[B](f: Tag[A] => B, g: A => B): B = asEither.fold(f, g)
  final def map[B](f: A => B): BinaryCheckVersion[B] =
    fold(
      tag => BinaryCheckVersion.fromTag(tag.map(f)),
      value => BinaryCheckVersion.fromNonTag(f(value))
    )

  final def emap[B](f: A => Either[String, B]): Either[String, BinaryCheckVersion[B]] =
    asEither.fold(
      tag => tag.emap(f).map(BinaryCheckVersion.fromTag),
      value => f(value).map(BinaryCheckVersion.fromNonTag)
    )

  override final def toString: String =
    fold(
      tag => s"BinaryCheckVersion(${tag})",
      value => s"BinaryCheckVersion($value)"
    )
}

object BinaryCheckVersion {
  private[this] final case class BinaryCheckVersionImpl[A](override final val asEither: Either[Tag[A], A]) extends BinaryCheckVersion[A]

  def fromEither[A](value: Either[Tag[A], A]): BinaryCheckVersion[A] =
    BinaryCheckVersionImpl(value)

  def fromTag[A](value: Tag[A]): BinaryCheckVersion[A] =
    fromEither(Left(value))

  def fromNonTag[A](value: A): BinaryCheckVersion[A] =
    fromEither(Right(value))

  def applyVersionScheme(versionScheme: VersionScheme, value: BinaryCheckVersion[Version]): Either[String, BinaryCheckVersion[versionScheme.VersionType]] =
    value.emap(value => versionScheme.fromVersion(value))

  implicit val order1Instance: Order1[BinaryCheckVersion] =
    new Order1[BinaryCheckVersion] {
      override def liftCompare[A, B](compare: A => B => Int, x: BinaryCheckVersion[A], y: BinaryCheckVersion[B]): Int =
        (x.isTag, y.isTag) match {
          case (true, false) =>
            // Tags are arbitrarily > non-tags
            1
          case (false, true) =>
            -1
          case _ =>
            // x and y are either both tags or both not tags
            x.asTag.flatMap(x =>
              y.asTag.map(y =>
                Order1[Tag].liftCompare(compare, x, y)
              )
            ).getOrElse(
              // x and y are both not tags
              compare(x.underlyingVersion)(y.underlyingVersion)
            )
        }
    }

  implicit def orderingInstance[A](implicit A: Ordering[A]): Ordering[BinaryCheckVersion[A]] =
    Order1.orderingFromOrder1

  implicit def versionChangeTypeClassInstance[A](implicit A: VersionChangeTypeClass[A]): VersionChangeTypeClass[BinaryCheckVersion[A]] =
    A.contramap(_.underlyingVersion)

  implicit def versionSchemableClassInstance[F[_], A](implicit F: VersionSchemableClass[F, A]): VersionSchemableClass[Lambda[B => BinaryCheckVersion[F[B]]], A] =
    new VersionSchemableClass[Lambda[B => BinaryCheckVersion[F[B]]], A] {
      override def scheme(versionScheme: VersionScheme, value: BinaryCheckVersion[F[A]]): Either[String, BinaryCheckVersion[F[versionScheme.VersionType]]] =
        value.emap(value =>
          F.scheme(versionScheme, value)
        )
    }
}
