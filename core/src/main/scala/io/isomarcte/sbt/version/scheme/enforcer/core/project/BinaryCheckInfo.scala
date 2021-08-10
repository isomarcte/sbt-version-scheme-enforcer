package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.toSortedSet
import scala.collection.immutable.SortedSet

sealed abstract class BinaryCheckInfo[A, B] extends Product with Serializable {
  protected implicit def orderingA: Ordering[A]
  protected implicit def orderingB: Ordering[B]

  def checks: BinaryChecks[A]
  def invalidVersions: Option[SortedSet[B]]

  // final //

 final def withChecks[C: Ordering](value: BinaryChecks[C]): BinaryCheckInfo[C, B] =
    BinaryCheckInfo(value, invalidVersions)

  final def withInvalidVersions[C: Ordering](value: Option[Set[C]]): BinaryCheckInfo[A, C] =
    BinaryCheckInfo(checks, value)

  final def mapChecks[C: Ordering](f: BinaryChecks[A] => BinaryChecks[C]): BinaryCheckInfo[C, B] =
    withChecks(f(checks))

  final def mapInvalidVersions[C: Ordering](f: Option[Set[B]] => Option[Set[C]]): BinaryCheckInfo[A, C] =
    withInvalidVersions(f(invalidVersions))

  final def addChecks(value: BinaryChecks[A]): BinaryCheckInfo[A, B] =
    mapChecks(_ ++ value)

  final def addInvalidVersions(value: Set[B]): BinaryCheckInfo[A, B] =
    withInvalidVersions(
      Some(
        invalidVersions.fold(
          toSortedSet(value)
        )(_ ++ value)
      )
    )

  final def addInvalidTag(value: B): BinaryCheckInfo[A, B] =
    addInvalidVersions(SortedSet(value))

  final def ++(that: BinaryCheckInfo[A, B]): BinaryCheckInfo[A, B] =
    BinaryCheckInfo(checks ++ that.checks,
      invalidVersions.flatMap(a => that.invalidVersions.map(b => a ++ b)) orElse invalidVersions orElse that.invalidVersions
    )

  override final def toString: String =
    s"BinaryCheckInfo(checks = ${checks}, invalidVersions = ${invalidVersions})"
}

object BinaryCheckInfo {
  private[this] final case class BinaryCheckInfoImpl[A, B](override val checks: BinaryChecks[A], override val invalidVersions: Option[SortedSet[B]], _orderingA: Ordering[A], _orderingB: Ordering[B]) extends BinaryCheckInfo[A, B] {
    override protected implicit def orderingA: Ordering[A] = _orderingA
    override protected implicit def orderingB: Ordering[B] = _orderingB
  }

  def empty[A: Ordering, B: Ordering]: BinaryCheckInfo[A, B] = apply[A, B](BinaryChecks.empty[A], None)

  def apply[A, B](checks: BinaryChecks[A], invalidVersions: Option[Set[B]])(implicit A: Ordering[A], B: Ordering[B]): BinaryCheckInfo[A, B] =
    BinaryCheckInfoImpl(checks, invalidVersions.map(value => toSortedSet[B](value)), A, B)

  def applyVersionScheme(versionScheme: VersionScheme, value: SBTBinaryCheckInfoV[Version]): Either[String, SBTBinaryCheckInfoV[versionScheme.VersionType]] = {
    implicit val orderingInstance: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
    BinaryChecks.applyVersionScheme(versionScheme, value.checks).map(checks =>
      value.withChecks(checks)
    )
  }

  def partitionFromSchemeAndProjectVersionInfo(versionScheme: VersionScheme)(projectVersionInfo: ProjectVersionInfo[Version]): Either[String, Option[BinaryCheckInfo[versionScheme.VersionType, BinaryCheckVersion[Version]]]] = {
    implicit val orderingInstance: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
    implicit val versionChangeTypeClassInstance: VersionChangeTypeClass[versionScheme.VersionType] = versionScheme.versionTypeVersionChangeTypeClassInstance
    ProjectVersionInfo.applyVersionSchemeSplitTags(versionScheme, projectVersionInfo).map{
      case (projectVersionInfo, invalidVersions) =>
        BinaryChecks.partitionFromProjectVersionInfo(projectVersionInfo).map(checks =>
          BinaryCheckInfo(checks, invalidVersions.map(value => value.map(tag => BinaryCheckVersion.fromTag[Version](tag))))
        )
    }
  }
}
