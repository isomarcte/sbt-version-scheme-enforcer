package io.isomarcte.sbt.version.scheme.enforcer.plugin

import cats.effect._
import com.typesafe.tools.mima.plugin._
import io.isomarcte.sbt.version.scheme.enforcer.core._
import sbt.Keys._
import sbt._

object SbtVersionSchemeEnforcerPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = MimaPlugin

  object autoImport extends Keys
  import autoImport._

  override def buildSettings: Seq[Def.Setting[_]] =
    Seq(
      versionSchemeEnforcerPreviousVersion := {
        val currentValue: Option[String] = versionSchemeEnforcerPreviousVersion.?.value.flatten
        if (currentValue.isEmpty) {
          SbtVersionSchemeEnforcer.previousTagFromGit[SyncIO].attempt.unsafeRunSync.toOption.flatten
        } else {
          currentValue
        }
      }
    )

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      versionSchemeEnforcerChangeType := {
        val previousVersion: Option[String] = versionSchemeEnforcerPreviousVersion.value
        val currentVersion: String          = version.value
        val scheme: Option[String]          = versionScheme.?.value.flatten
        SbtVersionSchemeEnforcer
          .versionChangeTypeFromSchemeAndPreviousVersion[SyncIO](scheme, previousVersion, currentVersion)
          .attempt
          .unsafeRunSync
      },
      versionSchemeCheck := {
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
            Function.const("backward"),
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
      },
      MimaKeys.mimaPreviousArtifacts := {
        val currentValue: Set[ModuleID] = MimaKeys.mimaPreviousArtifacts.value
        if (currentValue.isEmpty && publishArtifact.in(Compile).value) {
          versionSchemeEnforcerPreviousVersion
            .value
            .fold(currentValue)(previousVersion => Set(organization.value %% name.value % previousVersion))
        } else {
          currentValue
        }
      }
    )
}
