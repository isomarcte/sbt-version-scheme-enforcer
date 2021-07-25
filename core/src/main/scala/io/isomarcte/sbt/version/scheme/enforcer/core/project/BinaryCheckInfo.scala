package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core.internal.setToSortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet

sealed abstract class BinaryCheckInfo[A] extends Product with Serializable {
  protected implicit def orderingInstance: Ordering[A]

  def checks: BinaryChecks[A]
  def invalidTags: SortedSet[Tag[A]]

  // final //

  final def withChecks(value: BinaryChecks[A]): BinaryCheckInfo[A] =
    BinaryCheckInfo(value, invalidTags)

  final def withInvalidTags(value: Set[Tag[A]]): BinaryCheckInfo[A] =
    BinaryCheckInfo(checks, value)

  final def mapChecks(f: BinaryChecks[A] => BinaryChecks[A]): BinaryCheckInfo[A] =
    withChecks(f(checks))

  final def addChecks(value: BinaryChecks[A]): BinaryCheckInfo[A] =
    mapChecks(_ ++ value)

  final def addInvalidTags(value: Set[Tag[A]]): BinaryCheckInfo[A] =
    withInvalidTags(invalidTags ++ value)

  final def addInvalidTag(value: Tag[A]): BinaryCheckInfo[A] =
    addInvalidTags(SortedSet(value))

  final def ++(that: BinaryCheckInfo[A]): BinaryCheckInfo[A] =
    BinaryCheckInfo(checks ++ that.checks, invalidTags ++ that.invalidTags)

  override final def toString: String =
    s"BinaryCheckInfo(checks = ${checks}, invalidTags = ${invalidTags})"
}

object BinaryCheckInfo {
  private[this] final case class BinaryCheckInfoImpl[A](override val checks: BinaryChecks[A], override val invalidTags: SortedSet[Tag[A]], ordering: Ordering[A]) extends BinaryCheckInfo[A] {
    override protected implicit def orderingInstance: Ordering[A] = ordering
  }

  def empty[A](implicit A: Ordering[A]): BinaryCheckInfo[A] = apply[A](BinaryChecks.empty[A], SortedSet.empty[Tag[A]])

  def apply[A](checks: BinaryChecks[A], invalidTags: Set[Tag[A]])(implicit A: Ordering[A]): BinaryCheckInfo[A] =
    BinaryCheckInfoImpl(checks, setToSortedSet(invalidTags), A)
}
