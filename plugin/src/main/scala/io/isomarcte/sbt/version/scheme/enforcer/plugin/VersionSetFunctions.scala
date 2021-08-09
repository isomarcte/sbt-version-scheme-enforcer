package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core.project._
import io.isomarcte.sbt.version.scheme.enforcer.core.project.BinaryCheckInfo.BinaryCheckInfoV
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._

object VersionSetFunctions {
  type BinaryCheckEF[A] = ProjectVersionInfo[A] => BinaryCheckInfoV[A] => Either[String, BinaryCheckInfoV[A]]
  type BinaryCheckF[A] = ProjectVersionInfo[A] => BinaryCheckInfoV[A] => BinaryCheckInfoV[A]
  type VersionSetEF[A] = VersionScheme => BinaryCheckEF[A]
  type VersionSetF[A] = VersionScheme => BinaryCheckF[A]

  def fromSchemedE(versionScheme: VersionScheme)(f: BinaryCheckEF[versionScheme.VersionType]): BinaryCheckEF[Version] =
    (projectVersionInfo: ProjectVersionInfo[Version]) => (binaryCheckInfo: BinaryCheckInfoV[Version]) => {
      ProjectVersionInfo.applyVersionSchemeSplitTags(versionScheme, projectVersionInfo).flatMap{
        case (projectVersionInfo, _) =>
          BinaryCheckInfo.applyVersionScheme(versionScheme, binaryCheckInfo).flatMap(binaryCheckInfo =>
            f(projectVersionInfo)(binaryCheckInfo).map(
              _.mapChecks(_.map(_.map(value => versionScheme.toVersion(value))))
            )
          )
      }
    }

  def fromSchemed(versionScheme: VersionScheme)(f: BinaryCheckF[versionScheme.VersionType]): BinaryCheckEF[Version] =
    fromSchemedE(versionScheme)(projectVersionInfo => binaryCheckInfo => Right(f(projectVersionInfo)(binaryCheckInfo)))

  private def validate(versionScheme: VersionScheme, projectInfo: ProjectVersionInfo[Version], checks: BinaryChecks[Tag[Version]]): Either[String, (ProjectVersionInfo[versionScheme.VersionType], BinaryChecks[Tag[versionScheme.VersionType]], Option[SortedSet[Tag[Version]]])] =
    for {
      (pi, invalid) <- ProjectVersionInfo.applyVersionSchemeSplitTags(versionScheme, projectInfo)
      chks <- BinaryChecks.applyVersionSchemeT(versionScheme, checks)
    } yield (pi, chks, invalid)

  private def validateF(versionScheme: VersionScheme, projectInfo: ProjectVersionInfo[Version], checks: BinaryChecks[Tag[Version]])(f: ProjectVersionInfo[versionScheme.VersionType] => BinaryChecks[Tag[versionScheme.VersionType]] => BinaryChecks[Tag[versionScheme.VersionType]]): Either[String, BinaryCheckInfo[Tag[Version], Tag[Version]]] =
    validate(versionScheme, projectInfo, checks).map{
      case (projectInfo, checks, invalid) =>
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        BinaryCheckInfo(
          f(projectInfo)(checks),
          invalid
        ).mapChecks(checks => checks.map(value => value.map(value => versionScheme.toVersion(value))))
    }

  def union[A](f: VersionSetEF[A], g: VersionSetEF[A]): VersionSetEF[A] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[A]) => (info: BinaryCheckInfoV[A]) => {
      for {
        a <- f(versionScheme)(projectInfo)(info)
        b <- g(versionScheme)(projectInfo)(info)
      } yield a ++ b
    }

  def composeChecks[A](f: VersionSetEF[A]): VersionSetEF[A] => VersionSetEF[A] =
    (g: VersionSetEF[A]) => (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[A]) => (info: BinaryCheckInfoV[A]) => {
      f(versionScheme)(projectInfo)(info).flatMap((info: BinaryCheckInfoV[A]) =>
        g(versionScheme)(projectInfo)(info).map(value => info.invalidVersions.fold(value)(value.addInvalidVersions))
      )
    }

  def mostRecentNTagsOnly(count: Int): VersionSetEF[Version] =
    (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
      (_: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: BinaryCheckInfoV[versionScheme.VersionType]) => {
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        implicit val versionChangeTypeClassInstance: VersionChangeTypeClass[versionScheme.VersionType] = versionScheme.versionTypeVersionChangeTypeClassInstance
        binCheckInfo.mapChecks(checks =>
          BinaryChecks.mostRecentNTagsOnly(checks, count)
        )
      }
    )

  def closestNByVersion(count: Int): VersionSetEF[Version] =
    (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
      (_: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: BinaryCheckInfoV[versionScheme.VersionType]) => {
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        binCheckInfo.mapChecks(_.maxN(count))
      }
    )

  val lessThanCurrentVersion: VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      validateF(versionScheme, projectInfo, checks){projectInfo => checks =>
        checks.lessThan(Tag(projectInfo.currentVersion))
      }
    }

  val greaterThanInitialVersion: VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      validateF(versionScheme, projectInfo, checks){projectInfo => checks =>
        projectInfo.initialVersion.fold(
          checks
        )(initialVersion =>
          checks.greaterThan(Tag(initialVersion))
        )
      }
    }

  def default(count: Int): VersionSetF[Version] =
    (composeChecks[Version](
      lessThanCurrentVersion
    ) andThen composeChecks(greaterThanInitialVersion))(
      union(mostRecentNTagsOnly(count), closestNByVersion(count))
    )
}
