package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

sealed abstract class SemanticVersion extends Ordered[SemanticVersion] {
  def major: BigInt
  def minor: BigInt
  def patch: BigInt

  def preReleaseComponents: Vector[VersionComponent.PreRelease]

  // Final //

  final def isPreRelease: Boolean = preReleaseComponents.nonEmpty

  final override def toString: String =
    s"SemanticVersion(major = ${major}, minor = ${minor}, patch = ${patch}, preReleaseComponents = ${preReleaseComponents})"

  final override def compare(that: SemanticVersion): Int =
    major.compare(that.major) match {
      case 0 =>
        minor.compare(that.minor) match {
          case 0 =>
            patch.compare(that.patch) match {
              case 0 =>
                if (isPreRelease && that.isPreRelease) {
                  preReleaseComponents.mkString.compare(that.preReleaseComponents.mkString)
                } else if (isPreRelease) {
                    -1
                } else {
                  1
                }
              case otherwise =>
                otherwise
            }
          case otherwise =>
            otherwise
        }
      case otherwise =>
        otherwise
    }

  final def changeType(that: SemanticVersion): VersionChangeType =
    if(major =!= that.major) {
      VersionChangeType.Major
    } else if (minor =!= that.minor){
      VersionChangeType.Minor
    } else {
      // Valid even if the versions are equal because a Patch change type
      // indicates no visible binary changes, which is true when comparing a
      // version to itself.
      VersionChangeType.Patch
    }
}

object SemanticVersion {

  private[this] final case class SemanticVersionImpl(
    override val major: BigInt,
    override val minor: BigInt,
    override val patch: BigInt,
    override val preReleaseComponents: Vector[VersionComponent.PreRelease]
  ) extends SemanticVersion

  def from(major: BigInt, minor: BigInt, patch: BigInt, preReleaseComponents: Vector[VersionComponent.PreRelease]): Either[String, SemanticVersion] =
    if (major >= 0 && minor >= 0 && patch >= 0) {
      Right(SemanticVersionImpl(major, minor, patch, preReleaseComponents))
    } else {
      Left(s"Semantic Versioning does not permit negative version number components. ${major}.${minor}.${patch}")
    }

  def from(major: BigInt, minor: BigInt, patch: BigInt): Either[String, SemanticVersion] =
    from(major, minor, patch, Vector.empty)

  def unsafeFrom(major: BigInt, minor: BigInt, patch: BigInt, preReleaseComponents: Vector[VersionComponent.PreRelease]): SemanticVersion =
    from(major, minor, patch, preReleaseComponents).fold(
      e => throw new IllegalArgumentException(e),
      identity
    )

  def unsafeFrom(major: BigInt, minor: BigInt, patch: BigInt): SemanticVersion =
    unsafeFrom(major, minor, patch, Vector.empty)
}
