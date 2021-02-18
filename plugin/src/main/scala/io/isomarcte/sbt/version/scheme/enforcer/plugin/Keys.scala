package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core._
import sbt._

trait Keys {

  final val versionSchemeEnforcerPreviousVersion: SettingKey[Option[String]] = settingKey[Option[String]](
    "Previous version to compare against the current version for calculating binary compatibility"
  )
  final val versionSchemeEnforcerChangeType: SettingKey[Option[Either[Throwable, VersionChangeType]]] =
    settingKey[Option[Either[Throwable, VersionChangeType]]](
      "The type of binary change. It is used to configured MiMa settings. Normally this is derived from versionSchemeEnforcerPreviousVersion and should not normally be set directly. If it results in an error and versionSchemeCheck is run, that error is raised. If the previous version is empty then this will be empty too in which case mima settings will not be altered in any way."
    )
  final val versionSchemeCheck: TaskKey[Unit] = taskKey[Unit](
    "Verifies that the sbt-version-scheme-enforcer settings are valid and runs MiMa with the derived settings. If versionSchemeEnforcerPreviousVersion is empty then this task will not run any binary checks or fail. Note, by default if using git versionSchemeEnforcerPreviousVersion is automatically derived, so if you want this behavior you need to explicitly set this. This can be particularly useful when adding new modules to multi-module builds."
  )
}

object Keys extends Keys
