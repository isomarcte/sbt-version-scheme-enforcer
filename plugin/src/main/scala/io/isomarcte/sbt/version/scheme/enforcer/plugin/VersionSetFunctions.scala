package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core.project._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._

object VersionSetFunctions {
  type VersionSetF[A] = VersionScheme => ProjectInfo[A] => BinaryChecks[Tag[A]] => Either[String, BinaryCheckInfo[Tag[A], Tag[Version]]]

  private def validate(versionScheme: VersionScheme, projectInfo: ProjectInfo[Version], checks: BinaryChecks[Tag[Version]]): Either[String, (ProjectInfo[versionScheme.VersionType], BinaryChecks[Tag[versionScheme.VersionType]], SortedSet[Tag[Version]])] =
    for {
      (pi, invalid) <- ProjectInfo.applyVersionSchemeSplitTags(versionScheme, projectInfo)
      chks <- BinaryChecks.applyVersionSchemeT(versionScheme, checks)
    } yield (pi, chks, invalid)

  private def validateF(versionScheme: VersionScheme, projectInfo: ProjectInfo[Version], checks: BinaryChecks[Tag[Version]])(f: ProjectInfo[versionScheme.VersionType] => BinaryChecks[Tag[versionScheme.VersionType]] => BinaryChecks[Tag[versionScheme.VersionType]]): Either[String, BinaryCheckInfo[Tag[Version], Tag[Version]]] =
    validate(versionScheme, projectInfo, checks).map{
      case (projectInfo, checks, invalid) =>
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        BinaryCheckInfo(
          f(projectInfo)(checks),
          invalid
        ).mapChecks(checks => checks.map(value => value.map(value => versionScheme.toVersion(value))))
    }

  def union(f: VersionSetF[Version], g: VersionSetF[Version]): VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      for {
        a <- f(versionScheme)(projectInfo)(checks)
        b <- g(versionScheme)(projectInfo)(checks)
      } yield a ++ b
    }

  val mostRecentTagsOnly: VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      validateF(versionScheme, projectInfo, checks){_ => (checks: BinaryChecks[Tag[versionScheme.VersionType]]) =>
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        implicit val versionChangeTypeClassInstance: VersionChangeTypeClass[versionScheme.VersionType] = versionScheme.versionTypeVersionChangeTypeClassInstance
        BinaryChecks.mostRecentTagsOnly(checks)
      }
    }

  val closestByVersion: VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      validateF(versionScheme, projectInfo, checks){_ => checks =>
        checks.max
      }
    }

  val lessThanCurrentVersion: VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      validateF(versionScheme, projectInfo, checks){projectInfo => checks =>
        checks.lessThan(Tag(projectInfo.currentVersion))
      }
    }

  val greaterThanInitialVersion: VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      validateF(versionScheme, projectInfo, checks){projectInfo => checks =>
        projectInfo.initialVersion.fold(
          checks
        )(initialVersion =>
          checks.greaterThan(Tag(initialVersion))
        )
      }
    }
}
