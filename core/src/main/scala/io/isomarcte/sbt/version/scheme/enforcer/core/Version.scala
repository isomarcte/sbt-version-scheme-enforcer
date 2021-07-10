package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

sealed abstract class Version extends Product with Serializable {
  def value: String

  // final //

  final def normalizeValue: String =
    value.trim.dropWhile(_ === 'v')

  override final def toString: String =
    s"Version(value = ${value})"
}

object Version {
  private[this] final case class VersionImpl(override val value: String) extends Version

  def apply(value: String): Version =
    VersionImpl(value)

  implicit val orderingInstance: Ordering[Version] =
    Ordering.by(_.value)
}
