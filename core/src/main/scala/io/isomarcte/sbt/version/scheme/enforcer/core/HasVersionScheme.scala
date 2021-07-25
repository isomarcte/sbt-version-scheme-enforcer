package io.isomarcte.sbt.version.scheme.enforcer.core

sealed trait VersionSchemeClass[A] extends Serializable {
  def versionScheme: VersionScheme
}

object VersionSchemeClass {
  implicit val pvpInstance: VersionSchemeClass[PVPVersion] =
    new VersionSchemeClass[PVPVersion] {
      override val versionScheme: VersionScheme = VersionScheme.PVP
    }

  implicit val semVerInstance: VersionSchemeClass[SemVerVersion] =
    new VersionSchemeClass[SemVerVersion] {
      override val versionScheme: VersionScheme = VersionScheme.SemVer
    }

  implicit val earlySemVerInstance: VersionSchemeClass[EarlySemVerVersion] =
    new VersionSchemeClass[EarlySemVerVersion] {
      override val versionScheme: VersionScheme = VersionScheme.EarlySemVer
    }

  implicit val strictInstance: VersionSchemeClass[StrictVersion] =
    new VersionSchemeClass[StrictVersion] {
      override val versionScheme: VersionScheme = VersionScheme.Strict
    }

  implicit val alwaysInstance: VersionSchemeClass[AlwaysVersion] =
    new VersionSchemeClass[AlwaysVersion] {
      override val versionScheme: VersionScheme = VersionScheme.Always
    }
}
