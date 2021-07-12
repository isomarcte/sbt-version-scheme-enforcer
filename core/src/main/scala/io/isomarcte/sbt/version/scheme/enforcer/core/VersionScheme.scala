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

  def changeType(versionScheme: VersionScheme)(x: Version, y: Version): Either[String, VersionChangeType] =
    versionScheme match {
      case PVP =>
        for {
          x <- PVPVersion.fromVersion(x)
          y <- PVPVersion.fromVersion(y)
        } yield VersionChangeTypeClass[PVPVersion].changeType(x, y)
      case SemVer =>
        for {
          x <- SemVerVersion.fromVersion(x)
          y <- SemVerVersion.fromVersion(y)
        } yield VersionChangeTypeClass[SemVerVersion].changeType(x, y)
      case EarlySemVer =>
        for {
          x <- EarlySemVerVersion.fromVersion(x)
          y <- EarlySemVerVersion.fromVersion(y)
        } yield VersionChangeTypeClass[EarlySemVerVersion].changeType(x, y)
      case Strict =>
        Right(VersionChangeTypeClass[StrictVersion].changeType(StrictVersion(x.value), StrictVersion(y.value)))
      case Always =>
        Right(VersionChangeTypeClass[AlwaysVersion].changeType(AlwaysVersion(x.value), AlwaysVersion(y.value)))
    }

  def changeTypeFromStrings(versionScheme: String)(x: String, y: String): Either[String, VersionChangeType] =
    VersionScheme.fromCoursierVersionString(versionScheme).fold(
      Left(s"Unknown or invalid versionScheme: ${versionScheme}"): Either[String, VersionChangeType]
    )(versionScheme =>
      changeType(versionScheme)(Version(x), Version(y))
    )

  def orderByScheme(versionScheme: VersionScheme)(x: Version, y: Version): Either[String, Int] =
    versionScheme match {
      case PVP =>
        for {
          x <- PVPVersion.fromVersion(x)
          y <- PVPVersion.fromVersion(y)
        } yield Ordering[PVPVersion].compare(x, y)
      case SemVer =>
        for {
          x <- SemVerVersion.fromVersion(x)
          y <- SemVerVersion.fromVersion(y)
        } yield Ordering[SemVerVersion].compare(x, y)
      case EarlySemVer =>
        for {
          x <- EarlySemVerVersion.fromVersion(x)
          y <- EarlySemVerVersion.fromVersion(y)
        } yield Ordering[EarlySemVerVersion].compare(x, y)
      case Always =>
        Right(Ordering[AlwaysVersion].compare(AlwaysVersion.fromVersion(x), AlwaysVersion.fromVersion(y)))
      case Strict =>
        Right(Ordering[StrictVersion].compare(StrictVersion.fromVersion(x), StrictVersion.fromVersion(y)))
    }
}
