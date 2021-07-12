package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core._
import scala.collection.immutable.SortedSet

sealed abstract class BinaryChecks extends Product with Serializable {
  def backwardChecks: SortedSet[Version]
  def forwardChecks: SortedSet[Version]
  def bothChecks: SortedSet[Version]

  // final //

  final def ++(that: BinaryChecks): BinaryChecks =
    BinaryChecks(
      backwardChecks ++ that.backwardChecks,
      forwardChecks ++ that.forwardChecks,
      bothChecks ++ that.bothChecks
    )

  final def addBackwardCheck(value: Version): BinaryChecks =
    BinaryChecks(backwardChecks ++ SortedSet(value), forwardChecks, bothChecks)

  final def addForwardCheck(value: Version): BinaryChecks =
    BinaryChecks(backwardChecks, forwardChecks ++ SortedSet(value), bothChecks)

  final def addBothCheck(value: Version): BinaryChecks =
    BinaryChecks(backwardChecks, forwardChecks, bothChecks ++ SortedSet(value))

  override final def toString: String =
    s"BinaryChecks(backwardChecks = ${backwardChecks}, forwardChecks = ${forwardChecks}, bothChecks = ${bothChecks})"
}

object BinaryChecks {
  private[this] final case class BinaryChecksImpl(override val backwardChecks: SortedSet[Version], override val forwardChecks: SortedSet[Version], override val bothChecks: SortedSet[Version]) extends BinaryChecks

  val empty: BinaryChecks = BinaryChecksImpl(SortedSet.empty, SortedSet.empty, SortedSet.empty)

  def apply(backwardChecks: SortedSet[Version], forwardChecks: SortedSet[Version], bothChecks: SortedSet[Version]): BinaryChecks =
    BinaryChecksImpl(backwardChecks, forwardChecks, bothChecks)
}
