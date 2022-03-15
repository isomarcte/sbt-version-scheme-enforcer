package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core.project.syntax.all._
import io.isomarcte.sbt.version.scheme.enforcer.core._

object BinaryCheckInfoFilters {

  def fromSchemedE(versionScheme: VersionScheme)(f: ProjectVersionInfo[versionScheme.VersionType] => Either[String, BinaryChecks[BinaryCheckVersion[versionScheme.VersionType]]]): ProjectVersionInfo[Version] => Either[String, BinaryChecks[BinaryCheckVersion[Version]]] =
    (value: ProjectVersionInfo[Version]) => value.scheme(versionScheme).flatMap((projectVersionInfo: ProjectVersionInfo[versionScheme.VersionType]) =>
      f(projectVersionInfo).map((checks: BinaryChecks[BinaryCheckVersion[versionScheme.VersionType]]) =>
        checks.map(_.map(versionScheme.toVersion))
      )
    )

  def fromSchemed(versionScheme: VersionScheme)(f: ProjectVersionInfo[versionScheme.VersionType] => BinaryChecks[BinaryCheckVersion[versionScheme.VersionType]]): ProjectVersionInfo[Version] => Either[String, BinaryChecks[BinaryCheckVersion[Version]]] =
    fromSchemedE(versionScheme)(a => Right(f(a)))

  // def mostRecentNTagsOnly(count: Int): ProjectVersionInfo[Version] => Either[String, BinaryChecks[BinaryCheckVersion[Version]]] =
  //   (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
  //     (_: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: SBTBinaryCheckInfoV[versionScheme.VersionType]) => {
  //       implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
  //       implicit val versionChangeTypeClassInstance: VersionChangeTypeClass[versionScheme.VersionType] = versionScheme.versionTypeVersionChangeTypeClassInstance
  //       binCheckInfo.mapChecks(checks =>
  //         BinaryChecks.mostRecentNTagsOnly(checks, count)
  //       )
  //     }
  //   )

  // def closestNByVersion(count: Int): SBTSchemedBinaryCheckFilterE[Version] =
  //   (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
  //     (_: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: SBTBinaryCheckInfoV[versionScheme.VersionType]) => {
  //       implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
  //       binCheckInfo.mapChecks(_.maxN(count))
  //     }
  //   )

  // val lessThanCurrentVersion: SBTSchemedBinaryCheckFilterE[Version] =
  //   (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
  //     (projectVersionInfo: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: SBTBinaryCheckInfoV[versionScheme.VersionType]) => {
  //       implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
  //       binCheckInfo.mapChecks(_.lessThan(BinaryCheckVersion.fromNonTag(projectVersionInfo.currentVersion)))
  //     }
  //   )

  // val greaterThanInitialVersion: SBTSchemedBinaryCheckFilterE[Version] =
  //   (versionScheme: VersionScheme) => fromSchemed(versionScheme)(
  //     (projectVersionInfo: ProjectVersionInfo[versionScheme.VersionType]) => (binCheckInfo: SBTBinaryCheckInfoV[versionScheme.VersionType]) => {
  //       implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
  //       projectVersionInfo.initialVersion.fold(
  //         binCheckInfo
  //       )(initialVersion =>
  //         binCheckInfo.mapChecks(_.greaterThan(BinaryCheckVersion.fromNonTag(initialVersion)))
  //       )
  //     }
  //   )

  // def default(count: Int): SBTSchemedBinaryCheckFilterE[Version] =
  //   (composeChecks[Version](
  //     lessThanCurrentVersion
  //   ) andThen composeChecks(greaterThanInitialVersion))(
  //     union(mostRecentNTagsOnly(count), closestNByVersion(count))
  //   )
}
