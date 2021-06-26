package io.isomarcte.sbt.version.scheme.enforcer.plugin

import com.typesafe.tools.mima.plugin._
import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.SbtVersionSchemeEnforcer._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.vcs._
import sbt.Keys._
import sbt._
import scala.annotation.nowarn

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
      versionSchemeEnforcerPreviousTagFilter := Function.const(true)
    )

  override def buildSettings: Seq[Def.Setting[_]] =
    Seq(
      versionSchemeEnforcerPreviousVersion := {
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
                  .previousTagVersionsFiltered(versionSchemeEnforcerPreviousTagFilter.value)
                  .headOption
                  .fold(initialValue)(previousTag => Some(previousTag.versionString))
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
}
