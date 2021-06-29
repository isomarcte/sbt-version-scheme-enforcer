package io.isomarcte.sbt.version.scheme.enforcer.core

sealed abstract class SemanticVersionWithMetaData {
  def semanticVersion: SemanticVersion
  def metaData: Vector[VersionComponent.MetaData]

  // Final //

  override final def toString: String =
    s"SemanticVersionWithMetaData(semanticVersion = ${semanticVersion}, metaData = ${metaData})"
}

object SemanticVersionWithMetaData {
  private[this] final case class SemanticVersionWithMetaDataImpl(
    override val semanticVersion: SemanticVersion,
    override val metaData: Vector[VersionComponent.MetaData]
  ) extends SemanticVersionWithMetaData

  private[this] val semanticVersionDefinition: String =
    "A semantic version number may only have three sections. The number containing _exactly_ 3 version numbers, e.g. 1.2.3. An optional pre-release value containing 0 or more '.' separated pre-release identifiers. An optional metadata section containing 0 or '.' separated metadata identifiers."

  def from(value: Vector[VersionComponent]): Either[String, SemanticVersionWithMetaData] =
    value.toList match {
      case (major: VersionComponent.NonNegativeIntegral) :: (minor: VersionComponent.NonNegativeIntegral) :: (patch: VersionComponent.NonNegativeIntegral) :: rest =>
        val (preRelease, a) = rest.span{
          case (_: VersionComponent.PreRelease) => true
          case _ => false
        }

        val (metaData, b) = a.span{
          case (_: VersionComponent.MetaData) => true
          case _ => false
        }

        // Should never fail
        preRelease.foldLeft(Right(Vector.empty[VersionComponent.PreRelease]): Either[String, Vector[VersionComponent.PreRelease]]){
          case (acc, value: VersionComponent.PreRelease) =>
            acc.map(
              _ ++ Vector(value)
            )
          case (acc, otherwise) =>
            acc.flatMap(
              Function.const(Left(s"Expected VersionComponent.PreRelease, but got ${otherwise}. This is a bug in sbt-version-scheme-enforcer-core, please report it."))
            )
        }.flatMap(preRelease =>
          metaData.foldLeft(Right(Vector.empty[VersionComponent.MetaData]): Either[String, Vector[VersionComponent.MetaData]]){
            case (acc, value: VersionComponent.MetaData) =>
              acc.map(
                _ ++ Vector(value)
              )
            case (acc, otherwise) =>
              acc.flatMap(
                Function.const(Left(s"Expected VersionComponent.MetaData, but got ${otherwise}. This is a bug in sbt-version-scheme-enforcer-core, please report it."))
              )
          }.flatMap(metaData =>
            if (b.nonEmpty) {
              Left(s"Invalid semantic version: Found values after parsing metadata for ${value}. ${semanticVersionDefinition}")
            } else {
              SemanticVersion.from(
                major.asBigInt, minor.asBigInt, patch.asBigInt, preRelease
              ).map(semanticVersion =>
                SemanticVersionWithMetaDataImpl(
                  semanticVersion,
                  metaData
                )
              )
            }
          )
        )
      case _ =>
        Left(s"Invalid semantic version: Found not exactly 3 non-negative numeric version components ${value}. ${semanticVersionDefinition}")
    }

  def fromVersion(value: Version): Either[String, SemanticVersionWithMetaData] =
    from(value.components)

  def fromString(value: String): Either[String, SemanticVersionWithMetaData] =
    fromVersion(Version(value))
}
