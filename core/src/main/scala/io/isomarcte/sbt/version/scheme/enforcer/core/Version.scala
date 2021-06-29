package io.isomarcte.sbt.version.scheme.enforcer.core

sealed abstract class Version extends Product with Serializable {
  def value: String

  // Final //

  final lazy val components: Vector[VersionComponent] =
    VersionComponent.fromVersionString(value)

  override final def toString: String = s"Version(value = ${value})"
}

object Version {
  private[this] final case class VersionImpl(override val value: String) extends Version

  def apply(value: String): Version =
    VersionImpl(value)
}
