package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core.internal.setToSortedSet
import scala.collection.immutable.SortedSet

sealed abstract class BinaryCheckInfo[A, B] extends Product with Serializable {
  protected implicit def orderingA: Ordering[A]
  protected implicit def orderingB: Ordering[B]

  def checks: BinaryChecks[A]
  def invalidTags: SortedSet[B]

  // final //

  final def withChecks[C: Ordering](value: BinaryChecks[C]): BinaryCheckInfo[C, B] =
    BinaryCheckInfo(value, invalidTags)

  final def withInvalidTags[C: Ordering](value: Set[C]): BinaryCheckInfo[A, C] =
    BinaryCheckInfo(checks, value)

  final def mapChecks[C: Ordering](f: BinaryChecks[A] => BinaryChecks[C]): BinaryCheckInfo[C, B] =
    withChecks(f(checks))

  final def mapInvalidTags[C: Ordering](f: Set[B] => Set[C]): BinaryCheckInfo[A, C] =
    withInvalidTags(f(invalidTags))

  final def addChecks(value: BinaryChecks[A]): BinaryCheckInfo[A, B] =
    mapChecks(_ ++ value)

  final def addInvalidTags(value: Set[B]): BinaryCheckInfo[A, B] =
    withInvalidTags(invalidTags ++ value)

  final def addInvalidTag(value: B): BinaryCheckInfo[A, B] =
    addInvalidTags(SortedSet(value))

  final def ++(that: BinaryCheckInfo[A, B]): BinaryCheckInfo[A, B] =
    BinaryCheckInfo(checks ++ that.checks, invalidTags ++ that.invalidTags)

  override final def toString: String =
    s"BinaryCheckInfo(checks = ${checks}, invalidTags = ${invalidTags})"
}

object BinaryCheckInfo {
  private[this] final case class BinaryCheckInfoImpl[A, B](override val checks: BinaryChecks[A], override val invalidTags: SortedSet[B], _orderingA: Ordering[A], _orderingB: Ordering[B]) extends BinaryCheckInfo[A, B] {
    override protected implicit def orderingA: Ordering[A] = _orderingA
    override protected implicit def orderingB: Ordering[B] = _orderingB
  }

  def empty[A: Ordering, B: Ordering]: BinaryCheckInfo[A, B] = apply[A, B](BinaryChecks.empty[A], SortedSet.empty[B])

  def apply[A, B](checks: BinaryChecks[A], invalidTags: Set[B])(implicit A: Ordering[A], B: Ordering[B]): BinaryCheckInfo[A, B] =
    BinaryCheckInfoImpl(checks, setToSortedSet(invalidTags), A, B)
}
