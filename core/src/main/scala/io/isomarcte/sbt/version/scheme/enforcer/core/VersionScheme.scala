package io.isomarcte.sbt.version.scheme.enforcer.core

sealed abstract class VersionScheme extends Product with Serializable

object VersionScheme {
  case object PVP extends VersionScheme

  case object SemVer extends VersionScheme

  case object EarlySemVer extends VersionScheme

  case object Strict extends VersionScheme

  case object Always extends VersionScheme

  def fromCoursierVersionString(value: String): Option[VersionScheme] =
    value.trim.toLowerCase match {
      case "default" | "pvp" => Some(PVP)
      case "always" => Some(Always)
      case "strict" => Some(Strict)
      case "early-semver" | "semver" => Some(EarlySemVer)
      case "semver-spec" => Some(SemVer)
      case _ => None
    }

  implicit val orderingInstance: Ordering[VersionScheme] =
    Ordering.by(_.toString)
}
