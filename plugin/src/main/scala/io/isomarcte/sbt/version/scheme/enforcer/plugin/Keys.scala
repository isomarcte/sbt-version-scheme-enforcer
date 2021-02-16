package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core._
import sbt._

trait Keys {

  final val versionSchemeEnforcerPreviousVersion: SettingKey[Option[String]] = settingKey[Option[String]](
    "Previous version to compare against the current version for calculating binary compatibility"
  )
  final val versionSchemeEnforcerChangeType: SettingKey[Either[Throwable, VersionChangeType]] =
    settingKey[Either[Throwable, VersionChangeType]](
      "The type of binary change. It is used to configured MiMa settings. Normally this is derived from versionSchemeEnforcerPreviousVersion and should not normally be set directly. If it results in an error and versionSchemeCheck is run, that error is raised."
    )
  final val versionSchemeCheck: TaskKey[Unit] = taskKey[Unit](
    "Validates that the sbt-version-scheme-enforcer settings are valid and runs MiMa with the derived settings"
  )
}

object Keys extends Keys
