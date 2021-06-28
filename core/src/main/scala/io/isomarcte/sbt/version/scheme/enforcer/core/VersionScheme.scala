package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._

sealed abstract class VersionScheme extends Product with Serializable {
  def asString: String
}

object VersionScheme {

  case object PVP extends VersionScheme {
    override val asString: String = "pvp"
  }

  case object SemVer extends VersionScheme {
    override val asString: String = "semver"
  }

  case object EarlySemVer extends VersionScheme {
    override val asString: String = "early-semver"
  }

  sealed abstract class Unknown extends VersionScheme {
    override def toString: String = s"Unknown(asString = ${asString})"
  }

  private[this] object Unknown {
    final case class UnknownImpl(override val asString: String) extends Unknown
  }

  private[enforcer] def fromCoursierVersionCompatibility(
    value: VersionCompatibility
  ): VersionScheme = {
    value match {
      case VersionCompatibility.Default | VersionCompatibility.PackVer => PVP
      case VersionCompatibility.EarlySemVer => EarlySemVer
      case VersionCompatibility.SemVerSpec => SemVer
      case otherwise =>
        Unknown.UnknownImpl(otherwise.name)
    }
  }
}
