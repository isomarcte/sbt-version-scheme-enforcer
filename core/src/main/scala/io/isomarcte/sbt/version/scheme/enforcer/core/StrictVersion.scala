package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** A data type which represents a version scheme for which the visible API
  * can change any time the version string changes.
  *
  * I'm not sure how useful this is in practice. This data type exists because
  * coursier and SBT support it.
  *
  * @note There are not structural rules for this version. It may be an
  *       arbitrary [[java.lang.String]].
  */
sealed abstract class StrictVersion extends Product with Serializable {
  def value: String

  // final //

  override def toString: String = s"StrictVersion(${value})"
}

object StrictVersion {
  final private[this] case class StrictVersionImpl(override val value: String) extends StrictVersion

  def apply(value: String): StrictVersion = StrictVersionImpl(value)

  def fromVersion(value: Version): StrictVersion =
    apply(value.normalizeValue)

  implicit val orderingInstance: Ordering[StrictVersion] = Ordering.by(_.value)

  implicit val versionChangeTypeClassInstance: VersionChangeTypeClass[StrictVersion] =
    new VersionChangeTypeClass[StrictVersion] {
      override def changeType(x: StrictVersion, y: StrictVersion): VersionChangeType =
        if (x.value === y.value) {
          VersionChangeType.Patch
        } else {
          VersionChangeType.Major
        }
    }
}
