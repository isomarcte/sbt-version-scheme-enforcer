package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core._
import sbt._

trait Keys {

  // Settings

  final val versionSchemeEnforcerPreviousVersion: SettingKey[Option[String]] = settingKey[Option[String]](
    "Previous version to compare against the current version for calculating binary compatibility. If this is not set manually and can not be derived from the Version Control System (VCS), then it will default to the value of versionSchemeEnforcerInitialVersion."
  )

  final val versionSchemeEnforcerChangeType: SettingKey[Either[Throwable, VersionChangeType]] =
    settingKey[Either[Throwable, VersionChangeType]](
      "The type of binary change. It is used to configured MiMa settings. Normally this is derived from versionSchemeEnforcerPreviousVersion and should not normally be set directly. If it results in an error and versionSchemeEnforcerCheck is run, that error is raised."
    )

  @deprecated(
    message =
      "DO NOT USE THIS. Use versionSchemeEnforcerInitialVersion instead. This key has a spelling error in its name (Intial -> Initial). If this is set and versionSchemeEnforcerInitialVersion is not set, this will be used as versionSchemeEnforcerInitialVersion. If versionSchemeEnforcerInitialVersion is set this will be ignored.",
    since = "2.1.0.0"
  )
  final val versionSchemeEnforcerIntialVersion: SettingKey[Option[String]] = settingKey[Option[String]](
    "DO NOT USE THIS. Use versionSchemeEnforcerInitialVersion instead. This key has a spelling error in its name (Intial -> Initial). If this is set and versionSchemeEnforcerInitialVersion is not set, this will be used as versionSchemeEnforcerInitialVersion. If versionSchemeEnforcerInitialVersion is set this will be ignored."
  )

  final val versionSchemeEnforcerInitialVersion: SettingKey[Option[String]] = settingKey[Option[String]](
    "The initial version which should have the versionScheme enforced. If this is set then verions <= to this version will have Mima configured to not validate any binary compatibility constraints. This is particularly useful when you are adding a new module to an exsiting project."
  )

  final val versionSchemeEnforcerPreviousTagFilter: SettingKey[String => Boolean] = settingKey[String => Boolean](
    "A filter used when determining the previous version from a VCS tag. The selected tag will be the most recent tag, reachable from the current commit, for which this filter returns true. A common use case for this is not considering pre-release in binary compatibility checks. For example, assuming your versionScheme is Semver or Early Semver, if you are releasing 1.1.0-M3, you may want to consider binary compatibility compared to the last 1.0.x release, and permit arbitrary binary changes between various milestone releases. By default, comparing two versions which have the same numeric base version will imply that no visible changes have been made to the binary API, e.g. comparing 1.1.0-M2 to 1.1.0-M3 will yield a binary change type of Patch (assuming Semver or Early Semver.)"
  )

  final val versionSchemeEnforcerTagDomain: SettingKey[TagDomain] = settingKey[TagDomain](
    "The domain of VCS tags to consider when looking for previous releases to use in the binary compatibility check. For example, this can be TagDomain.All to consider all tags on the repository, or TagDomain.Reachable to only consider tags which are reachable (ancestors) of the current commit. The later case can be useful when you have multiple branches which should not be considered directly related for the purposes of binary compatibility. TagDomain.All is the default as of 2.1.1.0. The behavior prior to 2.1.1.0 was equivalent to TagDomain.Reachable."
  )

  // Tasks

  final val versionSchemeEnforcerCheck: TaskKey[Unit] = taskKey[Unit](
    "Verifies that the sbt-version-scheme-enforcer settings are valid and runs MiMa with the derived settings."
  )
}

object Keys extends Keys
