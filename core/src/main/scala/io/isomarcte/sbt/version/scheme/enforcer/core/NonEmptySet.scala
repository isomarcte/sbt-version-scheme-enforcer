package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.collection.immutable.SortedSet

sealed abstract class NonEmptySet[A] {
  def head: A
  def rest: SortedSet[A]

  // Protected //

  protected implicit def orderingA: Ordering[A]

  // Final //

  override final def toString: String =
    s"NonEmptySet(head = ${head}, rest = ${rest})"

  final def asSortedSet: SortedSet[A] =
    SortedSet(head) ++ rest
}

object NonEmptySet {
  private final case class NonEmptySetImpl[A](override val head: A, override val rest: SortedSet[A], override protected implicit val orderingA: Ordering[A]) extends NonEmptySet[A]

  def apply[A](head: A, rest: SortedSet[A])(implicit A: Ordering[A]): NonEmptySet[A] =
    NonEmptySetImpl(head, rest, A)

  def one[A: Ordering](value: A): NonEmptySet[A] =
    apply(value, SortedSet.empty[A])

  def of[A: Ordering](head: A, rest: A*): NonEmptySet[A] =
    apply[A](head, SortedSet(rest: _*))
}
