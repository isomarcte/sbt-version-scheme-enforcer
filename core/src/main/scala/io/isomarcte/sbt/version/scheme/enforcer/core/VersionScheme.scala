package io.isomarcte.sbt.version.scheme.enforcer.core

sealed abstract class VersionScheme extends Product with Serializable {
  type VersionType
  def versionTypeOrderingInstance: Ordering[VersionType]
  def versionTypeVersionChangeTypeClassInstance: VersionChangeTypeClass[VersionType]
  def fromVersion(value: Version): Either[String, VersionType]
  def toVersion(value: VersionType): Version
}

object VersionScheme {
  case object PVP extends VersionScheme {
    override type VersionType = PVPVersion
    override val versionTypeOrderingInstance: Ordering[VersionType] = Ordering[VersionType]
    override val versionTypeVersionChangeTypeClassInstance: VersionChangeTypeClass[VersionType] = VersionChangeTypeClass[VersionType]
    override def fromVersion(value: Version): Either[String,VersionType] =
      PVPVersion.fromVersion(value)
    override def toVersion(value: VersionType): Version =
      Version(value.canonicalString)
  }

  case object SemVer extends VersionScheme {
    override type VersionType = SemVerVersion
    override val versionTypeOrderingInstance: Ordering[VersionType] = Ordering[VersionType]
    override val versionTypeVersionChangeTypeClassInstance: VersionChangeTypeClass[VersionType] = VersionChangeTypeClass[VersionType]
    override def fromVersion(value: Version): Either[String,VersionType] =
      SemVerVersion.fromVersion(value)
    override def toVersion(value: VersionType): Version =
      Version(value.canonicalString)
  }

  case object EarlySemVer extends VersionScheme {
    override type VersionType = EarlySemVerVersion
    override val versionTypeOrderingInstance: Ordering[VersionType] = Ordering[VersionType]
    override val versionTypeVersionChangeTypeClassInstance: VersionChangeTypeClass[VersionType] = VersionChangeTypeClass[VersionType]
    override def fromVersion(value: Version): Either[String,VersionType] =
      EarlySemVerVersion.fromVersion(value)
    override def toVersion(value: VersionType): Version =
      Version(value.canonicalString)
  }

  case object Strict extends VersionScheme {
    override type VersionType = StrictVersion
    override val versionTypeOrderingInstance: Ordering[VersionType] = Ordering[VersionType]
    override val versionTypeVersionChangeTypeClassInstance: VersionChangeTypeClass[VersionType] = VersionChangeTypeClass[VersionType]
    override def fromVersion(value: Version): Either[String,VersionType] =
      Right(StrictVersion.fromVersion(value))
    override def toVersion(value: VersionType): Version =
      Version(value.value)
  }

  case object Always extends VersionScheme {
    override type VersionType = AlwaysVersion
    override val versionTypeOrderingInstance: Ordering[VersionType] = Ordering[VersionType]
    override val versionTypeVersionChangeTypeClassInstance: VersionChangeTypeClass[VersionType] = VersionChangeTypeClass[VersionType]
    override def fromVersion(value: Version): Either[String,VersionType] =
      Right(AlwaysVersion.fromVersion(value))
    override def toVersion(value: VersionType): Version =
      Version(value.value)
  }

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

  def validateByScheme(versionScheme: VersionScheme)(x: Version): Either[String, Version] =
    versionScheme match {
      case PVP =>
        PVPVersion.fromVersion(x).map(Function.const(x))
      case SemVer =>
        SemVerVersion.fromVersion(x).map(Function.const(x))
      case EarlySemVer =>
        EarlySemVerVersion.fromVersion(x).map(Function.const(x))
      case Always | Strict =>
        // Always and Strict are both always valid by scheme
        Right(x)
    }
}
