package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core.project._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._

object VersionSetFunctions {
  type VersionSetF[A] = VersionScheme => ProjectVersionInfo[A] => BinaryChecks[Tag[A]] => Either[String, BinaryCheckInfo[Tag[A], Tag[Version]]]

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

  def union(f: VersionSetF[Version], g: VersionSetF[Version]): VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      for {
        a <- f(versionScheme)(projectInfo)(checks)
        b <- g(versionScheme)(projectInfo)(checks)
      } yield a ++ b
    }

  def composeFilter[A](f: VersionSetF[A]): VersionSetF[A] => VersionSetF[A] =
    (g: VersionSetF[A]) => (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[A]) => (checks: BinaryChecks[Tag[A]]) => {
      f(versionScheme)(projectInfo)(checks).flatMap((info: BinaryCheckInfo[Tag[A], Tag[Version]]) =>
        g(versionScheme)(projectInfo)(info.checks).map(value => info.invalidTags.fold(value)(value.addInvalidTags))
      )
    }

  def mostRecentNTagsOnly(count: Int): VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      validateF(versionScheme, projectInfo, checks){_ => (checks: BinaryChecks[Tag[versionScheme.VersionType]]) =>
        implicit val ordering: Ordering[versionScheme.VersionType] = versionScheme.versionTypeOrderingInstance
        implicit val versionChangeTypeClassInstance: VersionChangeTypeClass[versionScheme.VersionType] = versionScheme.versionTypeVersionChangeTypeClassInstance
        BinaryChecks.mostRecentNTagsOnly(checks, count)
      }
    }

  def closestNByVersion(count: Int): VersionSetF[Version] =
    (versionScheme: VersionScheme) => (projectInfo: ProjectVersionInfo[Version]) => (checks: BinaryChecks[Tag[Version]]) => {
      validateF(versionScheme, projectInfo, checks){_ => checks =>
        checks.maxN(count)
      }
    }

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
    (composeFilter[Version](
      lessThanCurrentVersion
    ) andThen composeFilter(greaterThanInitialVersion))(
      union(mostRecentNTagsOnly(count), closestNByVersion(count))
    )
}
