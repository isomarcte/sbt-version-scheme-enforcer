package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.toSortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.emapSortedSet
import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._

sealed abstract class BinaryChecks[A] extends Product with Serializable {
  protected implicit def orderingInstance: Ordering[A]

  def backwardChecks: SortedSet[A]
  def forwardChecks: SortedSet[A]
  def bothChecks: SortedSet[A]

  // final //

  final def ++(that: BinaryChecks[A]): BinaryChecks[A] =
    BinaryChecks(
      backwardChecks ++ that.backwardChecks,
      forwardChecks ++ that.forwardChecks,
      bothChecks ++ that.bothChecks
    )

  final def withBackwardChecks(value: SortedSet[A]): BinaryChecks[A] =
    BinaryChecks(
      backwardChecks = value,
      forwardChecks = forwardChecks,
      bothChecks = bothChecks
    )

  final def withForwardChecks(value: SortedSet[A]): BinaryChecks[A] =
    BinaryChecks(
      backwardChecks = backwardChecks,
      forwardChecks = value,
      bothChecks = bothChecks
    )

  final def withBothChecks(value: SortedSet[A]): BinaryChecks[A] =
    BinaryChecks(
      backwardChecks = backwardChecks,
      forwardChecks = forwardChecks,
      bothChecks = value
    )

  final def map[B: Ordering](f: A => B): BinaryChecks[B] =
    BinaryChecks(
      backwardChecks = backwardChecks.map(f),
      forwardChecks = forwardChecks.map(f),
      bothChecks = bothChecks.map(f)
    )

  final def emap[B: Ordering](f: A => Either[String, B]): Either[String, BinaryChecks[B]] =
    for {
      backward <- emapSortedSet(f)(backwardChecks)
      forward <- emapSortedSet(f)(forwardChecks)
      both <- emapSortedSet(f)(bothChecks)
    } yield BinaryChecks(backwardChecks = backward, forwardChecks = forward, bothChecks = both)

  final def mapChecks[B: Ordering](f: SortedSet[A] => SortedSet[B]): BinaryChecks[B] =
    BinaryChecks(
      backwardChecks = f(backwardChecks),
      forwardChecks = f(forwardChecks),
      bothChecks = f(bothChecks)
    )

  final def mapBackwardChecks(f: SortedSet[A] => SortedSet[A]): BinaryChecks[A] =
    withBackwardChecks(f(backwardChecks))

  final def mapForwardChecks(f: SortedSet[A] => SortedSet[A]): BinaryChecks[A] =
    withForwardChecks(f(forwardChecks))

  final def mapBothChecks(f: SortedSet[A] => SortedSet[A]): BinaryChecks[A] =
    withBothChecks(f(bothChecks))

  final def addBackwardCheck(value: A): BinaryChecks[A] =
    mapBackwardChecks(_ ++ SortedSet(value))

  final def addForwardCheck(value: A): BinaryChecks[A] =
    mapForwardChecks(_ ++ SortedSet(value))

  final def addBothCheck(value: A): BinaryChecks[A] =
    mapBothChecks(_ ++ SortedSet(value))

  final def filterChecks(f: A => Boolean): BinaryChecks[A] =
    this.mapChecks[A](
      _.filter(f)
    )

  final def lessThanOrEqual(value: A): BinaryChecks[A] = {
    val ordering: Ordering[A] = implicitly[Ordering[A]]
    import ordering.mkOrderingOps
    this.filterChecks(
      _ <= value
    )
  }

  final def lessThan(value: A): BinaryChecks[A] = {
    val ordering: Ordering[A] = implicitly[Ordering[A]]
    import ordering.mkOrderingOps
    this.filterChecks(
      _ < value
    )
  }

  final def greaterThan(value: A): BinaryChecks[A] = {
    val ordering: Ordering[A] = implicitly[Ordering[A]]
    import ordering.mkOrderingOps
    this.filterChecks(
      _ > value
    )
  }

  final def maxN(value: Int): BinaryChecks[A] =
    mapChecks(checks =>
      toSortedSet(checks.toVector.takeRight(value))
    )

  final def max: BinaryChecks[A] =
    maxN(1)

  final def minN(value: Int): BinaryChecks[A] =
    mapChecks(
      _.take(value)
    )

  final def min: BinaryChecks[A] =
    minN(1)

  override final def toString: String =
    s"BinaryChecks(backwardChecks = ${backwardChecks}, forwardChecks = ${forwardChecks}, bothChecks = ${bothChecks})"
}

object BinaryChecks {
  private[this] final case class BinaryChecksImpl[A](override val backwardChecks: SortedSet[A], override val forwardChecks: SortedSet[A], override val bothChecks: SortedSet[A], override protected implicit val orderingInstance: Ordering[A]) extends BinaryChecks[A]

  def empty[A: Ordering]: BinaryChecks[A] = apply(SortedSet.empty, SortedSet.empty, SortedSet.empty)

  def apply[A](backwardChecks: SortedSet[A], forwardChecks: SortedSet[A], bothChecks: SortedSet[A])(implicit A: Ordering[A]): BinaryChecks[A] =
    BinaryChecksImpl(backwardChecks, forwardChecks, bothChecks, A)

  def applyVersionScheme(versionScheme: VersionScheme, binaryChecks: BinaryChecks[Version]): Either[String, BinaryChecks[versionScheme.VersionType]] = {
    implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
    binaryChecks.emap(value => versionScheme.fromVersion(value))
  }

  def applyVersionSchemeT(versionScheme: VersionScheme, binaryChecks: BinaryChecks[Tag[Version]]): Either[String, BinaryChecks[Tag[versionScheme.VersionType]]] = {
    implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
    binaryChecks.emap(value => value.emap(value => versionScheme.fromVersion(value)))
  }

  def partition[A](currentVersion: A, otherVersions: Set[A])(implicit A: Ordering[A], V: VersionChangeTypeClass[A]): BinaryChecks[A] =
    otherVersions.foldLeft(BinaryChecks.empty[A]){
      case (acc, value) =>
        V.changeType(currentVersion, value) match {
          case VersionChangeType.Major =>
            acc
          case VersionChangeType.Minor =>
            acc.addBackwardCheck(value)
          case VersionChangeType.Patch =>
            acc.addBothCheck(value)
        }
    }

  def partitionFromProjectVersionInfo[A: Ordering: VersionChangeTypeClass](projectVersionInfo: ProjectVersionInfo[A]): Option[BinaryChecks[A]] =
    projectVersionInfo.tags.map(tags =>
      BinaryChecks.partition(Tag(projectVersionInfo.currentVersion), tags).map(_.version)
    )

  def mostRecentTagsOnly[A: Ordering: VersionChangeTypeClass](checks: BinaryChecks[Tag[A]]): BinaryChecks[Tag[A]] =
    mostRecentNTagsOnly(checks, 1)

  def mostRecentNTagsOnly[A: Ordering: VersionChangeTypeClass](checks: BinaryChecks[Tag[A]], n: Int): BinaryChecks[Tag[A]] =
    checks.mapChecks(value =>
      toSortedSet(value.toVector.sorted(Tag.creationDateOrderingInstance.reverse).take(n))
    )
}
