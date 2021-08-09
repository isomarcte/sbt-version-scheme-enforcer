package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core.project._
import io.isomarcte.sbt.version.scheme.enforcer.core.project.BinaryCheckInfo.BinaryCheckInfoV
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

  val lessThanCurrentVersion: VersionSetEF[Version] =
    (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
      (projectVersionInfo: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: BinaryCheckInfoV[versionScheme.VersionType]) => {
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        binCheckInfo.mapChecks(_.lessThan(BinaryCheckVersion.fromNonTag(projectVersionInfo.currentVersion)))
      }
    )

  val greaterThanInitialVersion: VersionSetEF[Version] =
    (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
      (projectVersionInfo: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: BinaryCheckInfoV[versionScheme.VersionType]) => {
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        projectVersionInfo.initialVersion.fold(
          binCheckInfo
        )(initialVersion =>
          binCheckInfo.mapChecks(_.greaterThan(BinaryCheckVersion.fromNonTag(initialVersion)))
        )
      }
    )

  def default(count: Int): VersionSetEF[Version] =
    (composeChecks[Version](
      lessThanCurrentVersion
    ) andThen composeChecks(greaterThanInitialVersion))(
      union(mostRecentNTagsOnly(count), closestNByVersion(count))
    )
}
