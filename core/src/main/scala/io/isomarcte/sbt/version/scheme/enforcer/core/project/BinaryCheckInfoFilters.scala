package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core.project.syntax.all._
import io.isomarcte.sbt.version.scheme.enforcer.core._

object BinaryCheckInfoFilters {

  def fromSchemedE(versionScheme: VersionScheme)(f: SBTBinaryCheckInfoFilterE[versionScheme.VersionType]): SBTBinaryCheckInfoFilterE[Version] =
    {(projectVersionInfo: ProjectVersionInfo[Version]) =>
      projectVersionInfo.scheme(versionScheme)
    }
    // (projectVersionInfo: ProjectVersionInfo[Version]) => (binaryCheckInfo: SBTBinaryCheckInfoV[Version]) => {
    //   ProjectVersionInfo.applyVersionSchemeSplitTags(versionScheme, projectVersionInfo).flatMap{
    //     case (projectVersionInfo, _) =>
    //       BinaryCheckInfo.applyVersionScheme(versionScheme, binaryCheckInfo).flatMap(binaryCheckInfo =>
    //         f(projectVersionInfo)(binaryCheckInfo).map(
    //           _.mapChecks(_.map(_.map(value => versionScheme.toVersion(value))))
    //         )
    //       )
    //   }
    // }

  def fromSchemed(versionScheme: VersionScheme)(f: SBTBinaryCheckInfoFilter[versionScheme.VersionType]): SBTBinaryCheckInfoFilterE[Version] =
    fromSchemedE(versionScheme)(projectVersionInfo => binaryCheckInfo => Right(f(projectVersionInfo)(binaryCheckInfo)))

  def union[A](f: SBTSchemedBinaryCheckFilterE[A], g: SBTSchemedBinaryCheckFilterE[A]): SBTSchemedBinaryCheckFilterE[A] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[A]) => (info: SBTBinaryCheckInfoV[A]) => {
      for {
        a <- f(versionScheme)(projectInfo)(info)
        b <- g(versionScheme)(projectInfo)(info)
      } yield a ++ b
    }

  def composeChecks[A](f: SBTSchemedBinaryCheckFilterE[A]): SBTSchemedBinaryCheckFilterE[A] => SBTSchemedBinaryCheckFilterE[A] =
    (g: SBTSchemedBinaryCheckFilterE[A]) => (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[A]) => (info: SBTBinaryCheckInfoV[A]) => {
      f(versionScheme)(projectInfo)(info).flatMap((info: SBTBinaryCheckInfoV[A]) =>
        g(versionScheme)(projectInfo)(info).map(value => info.invalidVersions.fold(value)(value.addInvalidVersions))
      )
    }

  def mostRecentNTagsOnly(count: Int): SBTSchemedBinaryCheckFilterE[Version] =
    (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
      (_: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: SBTBinaryCheckInfoV[versionScheme.VersionType]) => {
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        implicit val versionChangeTypeClassInstance: VersionChangeTypeClass[versionScheme.VersionType] = versionScheme.versionTypeVersionChangeTypeClassInstance
        binCheckInfo.mapChecks(checks =>
          BinaryChecks.mostRecentNTagsOnly(checks, count)
        )
      }
    )

  def closestNByVersion(count: Int): SBTSchemedBinaryCheckFilterE[Version] =
    (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
      (_: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: SBTBinaryCheckInfoV[versionScheme.VersionType]) => {
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        binCheckInfo.mapChecks(_.maxN(count))
      }
    )

  val lessThanCurrentVersion: SBTSchemedBinaryCheckFilterE[Version] =
    (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
      (projectVersionInfo: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: SBTBinaryCheckInfoV[versionScheme.VersionType]) => {
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        binCheckInfo.mapChecks(_.lessThan(BinaryCheckVersion.fromNonTag(projectVersionInfo.currentVersion)))
      }
    )

  val greaterThanInitialVersion: SBTSchemedBinaryCheckFilterE[Version] =
    (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
      (projectVersionInfo: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: SBTBinaryCheckInfoV[versionScheme.VersionType]) => {
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        projectVersionInfo.initialVersion.fold(
          binCheckInfo
        )(initialVersion =>
          binCheckInfo.mapChecks(_.greaterThan(BinaryCheckVersion.fromNonTag(initialVersion)))
        )
      }
    )

  def default(count: Int): SBTSchemedBinaryCheckFilterE[Version] =
    (composeChecks[Version](
      lessThanCurrentVersion
    ) andThen composeChecks(greaterThanInitialVersion))(
      union(mostRecentNTagsOnly(count), closestNByVersion(count))
    )
}
