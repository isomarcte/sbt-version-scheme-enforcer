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

  implicit def orderingInstance[A](implicit A: Ordering[A]): Ordering[BinaryCheckVersion[A]] =
    new Ordering[BinaryCheckVersion[A]] {
      override def compare(x: BinaryCheckVersion[A], y: BinaryCheckVersion[A]): Int =
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
                Ordering[Tag[A]].compare(x, y)
              )
            ).getOrElse(
              // x and y are both not tags
              A.compare(x.underlyingVersion, y.underlyingVersion)
            )
        }
    }

  implicit def versionChangeTypeClassInstance[A](implicit A: VersionChangeTypeClass[A]): VersionChangeTypeClass[BinaryCheckVersion[A]] =
    A.contramap(_.underlyingVersion)

  implicit def versionSchemableClassInstance
}
