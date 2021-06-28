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

  case object Always extends VersionScheme {
    override val asString: String = "always"
  }

  case object Strict extends VersionScheme {
    override val asString: String = "strict"
  }

  // Typeclass Instances //

  implicit val orderingInstance: Ordering[VersionScheme] = Ordering.by(_.toString)

  // Private //

  private[enforcer] def fromString(value: String): Either[String, VersionScheme] = {
    value.trim.toLowerCase match {
      case PVP.asString | "default" =>
        Right(PVP)
      case EarlySemVer.asString =>
        Right(EarlySemVer)

      // Special case due to deprecated, but still valid, version scheme setup
      // in Coursier Versions.
      case SemVer.asString | "semver-spec" =>
        Right(SemVer)
      case Always.asString =>
        Right(Always)
      case Strict.asString =>
        Right(Strict)
      case otherwise =>
        Left(
          s"Unknown version scheme: ${otherwise}. If this scheme as well defined semantics, consider opening an issue and/or pull request to https://github.com/isomarcte/sbt-version-scheme-enforcer."
        )
    }
  }

  private[enforcer] def toCoursierVersionCompatibility(value: VersionScheme): VersionCompatibility =
    value match {
      case PVP =>
        VersionCompatibility.PackVer
      case EarlySemVer =>
        VersionCompatibility.EarlySemVer
      case SemVer =>
        VersionCompatibility.SemVerSpec
      case Always =>
        VersionCompatibility.Always
      case Strict =>
        VersionCompatibility.Strict
    }
}
