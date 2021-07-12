package io.isomarcte.sbt.version.scheme.enforcer.core

/** A data type which represents a version scheme for which the visible API
  * must be the same for all versions.
  *
  * I'm not sure how useful this is in practice. This data type exists because
  * coursier and SBT support it.
  *
  * "Always" as in "Always compatible".
  *
  * @note There are not structural rules for this version. It may be an
  *       arbitrary [[java.lang.String]].
  */
sealed abstract class AlwaysVersion extends Product with Serializable {
  def value: String

  // final //

  override def toString: String = s"AlwaysVersion(${value})"
}

object AlwaysVersion {
  final private[this] case class AlwaysVersionImpl(override val value: String) extends AlwaysVersion

  def apply(value: String): AlwaysVersion = AlwaysVersionImpl(value)

  def fromVersion(value: Version): AlwaysVersion =
    apply(value.normalizeValue)

  implicit val orderingInstance: Ordering[AlwaysVersion] = Ordering.by(_.value)

  implicit val versionChangeTypeClassInstance: VersionChangeTypeClass[AlwaysVersion] =
    new VersionChangeTypeClass[AlwaysVersion] {
      override def changeType(x: AlwaysVersion, y: AlwaysVersion): VersionChangeType = VersionChangeType.Patch
    }
}
