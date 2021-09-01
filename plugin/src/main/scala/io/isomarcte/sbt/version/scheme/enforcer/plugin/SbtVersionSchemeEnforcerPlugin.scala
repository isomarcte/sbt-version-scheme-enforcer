package io.isomarcte.sbt.version.scheme.enforcer.plugin

import com.typesafe.tools.mima.plugin._
import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.project._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.SbtVersionSchemeEnforcer._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.vcs._
import sbt.Keys._
import sbt._
import scala.annotation.nowarn
import scala.collection.immutable.SortedSet
import sbt.internal.util.ManagedLogger

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
      versionSchemeEnforcerTagDomain := TagDomain.All,
      versionSchemeEnforcerDeriveFromVCS := true,
      versionSchemeEnforcerVCSTags := {
        val s: TaskStreams = streams.value
        val tagDomain: TagDomain =
          versionSchemeEnforcerTagDomain.value
        if (versionSchemeEnforcerDeriveFromVCS.value) {
          VCS.determineVCSOption.fold(
            Option.empty[SortedSet[Tag[Version]]]
          )((vcs: VCS) =>
            vcs.tags(tagDomain).fold(
              e => {s.log.error(e.getLocalizedMessage); Option.empty[SortedSet[Tag[Version]]]},
              value => Some(value)
            )
          )
        } else {
          None
        }
      }
    )

  override def buildSettings: Seq[Def.Setting[_]] =
    Seq(
      versionSchemeEnforcerPreviousVersion := {
        val filter: Tag[Version] => Boolean = determineVCSTagFilter(
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
                  .fold(initialValue)(previousTag => Some(previousTag.version.value))
            )
        }
      }
    )

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      versionSchemeEnforcerPreviousVersions := {
        val previousVersion: Option[String] =
          versionSchemeEnforcerPreviousVersion.?.value.flatten
        val tags: Option[SortedSet[Tag[Version]]] =
          versionSchemeEnforcerVCSTags.value
        val current: Option[SortedSet[BinaryCheckVersion[Version]]] =
          versionSchemeEnforcerPreviousVersions.?.value.flatten
        type Result = BinaryCheckVersion[Version]

        if (previousVersion.isDefined || tags.isDefined || current.isDefined) {
          Some(previousVersion.map(value => SortedSet(BinaryCheckVersion.fromNonTag(Version(value)))).getOrElse(SortedSet.empty[Result]) ++
          tags.map(_.map(value => BinaryCheckVersion.fromTag(value))).getOrElse(SortedSet.empty[Result]) ++
          current.getOrElse(SortedSet.empty[Result]))
        } else {
          // None of the possible inputs for this are set, so None is returned
          // to differentiate between set, but empty and non-set states.
          None
        }
      },
      versionSchemeEnforcerBinaryCheckInfo := {
        val s: TaskStreams = streams.value
        validateVersionScheme(versionScheme.value).flatMap{versionScheme =>
          versionSchemeEnforcerPreviousVersions.value.fold(
            SortedSet.empty[BinaryCheckVersion[]]
          )
        }
      },
      versionSchemeEnforcerProjectVersionInfo := {
        val v: Version = Version(version.value)
        val iv: Option[Version] = versionSchemeEnforcerInitialVersion.value.map(Version.apply)
        val tags: Option[SortedSet[Tag[Version]]] = versionSchemeEnforcerVCSTags.value
        ProjectVersionInfo(v, iv, tags)
      },
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

  private[this] def liftStringFilterToTagFilter(f: String => Boolean): Tag[Version] => Boolean = (t: Tag[Version]) => f(t.version.value)

  private[this] def determineVCSTagFilterE(
    vcsTagFilter: Option[Tag[Version] => Boolean],
    vcsTagStringFilter: Option[String => Boolean],
    tagFilter: Option[String => Boolean]
  ): Either[Throwable, Tag[Version] => Boolean] =
    (
      vcsTagFilter.map(value => ("versionSchemeEnforcerPreviousVCSTagFilter", value)).toList ++
        vcsTagStringFilter
          .map(value => ("versionSchemeEnforcerPreviousVCSTagStringFilter", liftStringFilterToTagFilter(value)))
          .toList ++
        tagFilter.map(value => ("versionSchemeEnforcerPreviousTagFilter", liftStringFilterToTagFilter(value))).toList
    ).foldLeft((SortedSet.empty[String], ((_: Tag[Version]) => true))) { case ((filterNames, _), (name, filter)) =>
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
        ): Either[Throwable, Tag[Version] => Boolean]
      case (_, filter) =>
        Right(filter)
    }

  private[this] def determineVCSTagFilter(
    vcsTagFilter: Option[Tag[Version] => Boolean],
    vcsTagStringFilter: Option[String => Boolean],
    tagFilter: Option[String => Boolean]
  ): Tag[Version] => Boolean = determineVCSTagFilterE(vcsTagFilter, vcsTagStringFilter, tagFilter).fold(e => throw e, identity)
}
