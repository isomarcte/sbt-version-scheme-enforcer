package io.isomarcte.sbt.version.scheme.enforcer.plugin

import com.typesafe.tools.mima.plugin._
import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.SbtVersionSchemeEnforcer._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.vcs._
import sbt.Keys._
import sbt._
import scala.annotation.nowarn
import scala.collection.immutable.SortedSet

object SbtVersionSchemeEnforcerPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = MimaPlugin

  object autoImport extends Keys
  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] =
    Seq(
      (versionSchemeEnforcerIntialVersion := None: @nowarn("cat=deprecation")),
      versionSchemeEnforcerInitialVersion := None,
      versionSchemeEnforcerPreviousVersion := None,
      versionSchemeEnforcerTagDomain := TagDomain.All
    )

  override def buildSettings: Seq[Def.Setting[_]] =
    Seq(
      versionSchemeEnforcerPreviousVersion := {
        val filter: String => SortedSet[Tag] => Boolean = determineVCSTagFilter(
          versionSchemeEnforcerPreviousVCSTagFilter.?.value,
          versionSchemeEnforcerPreviousVCSTagStringFilter.?.value,
          versionSchemeEnforcerPreviousTagFilter.?.value: @nowarn("cat=deprecation")
        )
        val currentValue: Option[String] = versionSchemeEnforcerPreviousVersion.?.value.flatten
        val initialValue: Option[String] = versionSchemeEnforcerInitialVersion
          .?
          .value
          .flatten
          .orElse(versionSchemeEnforcerIntialVersion.?.value.flatten: @nowarn("cat=deprecation"))
        if (currentValue.isDefined) {
          currentValue
        } else {
          VCS
            .determineVCSE
            .fold(
              Function.const(currentValue),
              vcs =>
                vcs
                  .tagVersionsFiltered(filter, versionSchemeEnforcerTagDomain.value)
                  .toOption
                  .flatMap(_.lastOption)
                  .fold(initialValue)(previousTag => Some(previousTag.value))
            )
        }
      }
    )

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      versionSchemeEnforcerInitialVersion := {
        versionSchemeEnforcerInitialVersion
          .value
          .orElse(versionSchemeEnforcerIntialVersion.value: @nowarn("cat=deprecation"))
      },
      versionSchemeEnforcerChangeType := {
        val previousVersion: Option[String] = versionSchemeEnforcerPreviousVersion.?.value.flatten
        val currentVersion: String          = version.value
        val scheme: Option[String]          = versionScheme.?.value.flatten
        val initialVersion: Option[String] = versionSchemeEnforcerInitialVersion
          .?
          .value
          .flatten
          .orElse(versionSchemeEnforcerIntialVersion.?.value.flatten: @nowarn("cat=deprecation"))
        versionChangeTypeFromSchemeAndPreviousVersion(scheme, initialVersion, previousVersion, currentVersion)
      },
      MimaKeys.mimaReportBinaryIssues := {
        if (isAfterInitial(versionSchemeEnforcerInitialVersion.value, version.value, versionScheme.?.value.flatten)) {
          MimaKeys.mimaReportBinaryIssues.value
        } else {
          ()
        }
      },
      versionSchemeEnforcerCheck := {
        versionSchemeEnforcerChangeType
          .value
          .fold(e => sys.error(e.getLocalizedMessage), Function.const(MimaKeys.mimaReportBinaryIssues.value))
      },
      MimaKeys.mimaReportSignatureProblems := {
        versionSchemeEnforcerChangeType
          .value
          .fold(
            Function.const(MimaKeys.mimaReportSignatureProblems.value),
            {
              case VersionChangeType.Major =>
                false
              case VersionChangeType.Minor =>
                true
              case VersionChangeType.Patch =>
                true
            }
          )
      },
      MimaKeys.mimaCheckDirection := {
        versionSchemeEnforcerChangeType
          .value
          .fold(
            Function.const(MimaKeys.mimaCheckDirection.value),
            {
              case VersionChangeType.Major =>
                MimaKeys.mimaCheckDirection.value
              case VersionChangeType.Minor =>
                "backward"
              case VersionChangeType.Patch =>
                "both"
            }
          )
      },
      MimaKeys.mimaFailOnProblem := {
        versionSchemeEnforcerChangeType
          .value
          .fold(
            Function.const(MimaKeys.mimaFailOnProblem.value),
            {
              case VersionChangeType.Major =>
                false
              case _ =>
                true
            }
          )
      },
      MimaKeys.mimaFailOnNoPrevious := {
        val initialVersion: Option[String] = versionSchemeEnforcerInitialVersion.value
        val v: String                      = version.value
        if (isAfterInitial(initialVersion, v, versionScheme.?.value.flatten)) {
          versionSchemeEnforcerChangeType
            .value
            .fold(
              Function.const(MimaKeys.mimaFailOnNoPrevious.value),
              {
                case VersionChangeType.Major =>
                  false
                case _ =>
                  true
              }
            )
        } else {
          false
        }
      },
      MimaKeys.mimaPreviousArtifacts := {
        val shouldRun: Boolean = isAfterInitial(
          versionSchemeEnforcerInitialVersion.value,
          version.value,
          versionScheme.?.value.flatten
        )
        val currentValue: Set[ModuleID] = MimaKeys.mimaPreviousArtifacts.value
        if (shouldRun && currentValue.isEmpty && (Compile / publishArtifact).value) {
          versionSchemeEnforcerPreviousVersion
            .value
            .fold(currentValue)(previousVersion => Set(organization.value %% moduleName.value % previousVersion))
        } else {
          currentValue
        }
      }
    )

  // Private

  private[this] def liftStringFilterToTagFilter(f: String => Boolean): Tag => Boolean = (t: Tag) => f(t.value)

  private[this] def determineVCSTagFilterE(
    vcsTagFilter: Option[String => SortedSet[Tag] => Boolean],
    vcsTagStringFilter: Option[String => SortedSet[String] => Boolean],
    tagFilter: Option[String => Boolean]
  ): Either[Throwable, Tag => Boolean] =
    (
      vcsTagFilter.map(value => ("versionSchemeEnforcerPreviousVCSTagFilter", value)).toList ++
        vcsTagStringFilter
          .map(value => ("versionSchemeEnforcerPreviousVCSTagStringFilter", liftStringFilterToTagFilter(value)))
          .toList ++
        tagFilter.map(value => ("versionSchemeEnforcerPreviousTagFilter", liftStringFilterToTagFilter(value))).toList
    ).foldLeft((SortedSet.empty[String], ((_: Tag) => true))) { case ((filterNames, _), (name, filter)) =>
      // Add the filter name to the set of names, and replace the
      // filter. We will error if more than one filter is defined, so
      // replacing the current filter is fine.
      (filterNames ++ SortedSet(name), filter)
    } match {
      case (definedFilters, _) if definedFilters.size > 1 =>
        Left(
          new RuntimeException(
            s"""At most one of these settings may be defined, but ${definedFilters.mkString(", ")} were all defined."""
          )
        ): Either[Throwable, Tag => Boolean]
      case (_, filter) =>
        Right(filter)
    }

  private[this] def determineVCSTagFilter(
    vcsTagFilter: Option[String => SortedSet[Tag] => Boolean],
    vcsTagStringFilter: Option[String => SortedSet[String] => Boolean],
    tagFilter: Option[String => Boolean]
  ): String => SortedSet[Tag] => Boolean =
    determineVCSTagFilterE(vcsTagFilter, vcsTagStringFilter, tagFilter).fold(e => throw e, identity)
}
