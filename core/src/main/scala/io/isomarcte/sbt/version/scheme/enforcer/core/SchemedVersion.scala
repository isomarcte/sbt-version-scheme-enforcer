package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import coursier.version.{Version => CVersion}

/** A wrapper type for [[coursier.version.Version]] which properly handles PVP
  * ordering. As of io.get-coursier:versions_2.12:0.3.0 does not.
  */
sealed trait SchemedVersion extends Ordered[SchemedVersion] {
  import SchemedVersion._

  def scheme: VersionCompatibility
  def version: CVersion

  final override def compare(that: SchemedVersion): Int =
    (this.scheme, that.scheme) match {
      case (a, b) if isPVP(a) && isPVP(b) =>
        // Special case PVP because Coursier Version pads differing length
        // versions to the same length, but PVP explicitly says that versions
        // must be in strict lexicographic order, which implies that 1.0.0
        // < 1.0.0.0 _not_ equal.
        this.version.repr.compareTo(that.version.repr)
      case _ =>
        this.version.compare(that.version)
    }
}

object SchemedVersion {

  final private[this] case class SchemedVersionImpl(
    override val version: CVersion,
    override val scheme: VersionCompatibility
  ) extends SchemedVersion

  def fromVersionAndScheme(version: CVersion, scheme: VersionCompatibility): SchemedVersion =
    SchemedVersionImpl(version, scheme)

  def fromVersionStringAndScheme(version: String, scheme: VersionCompatibility): SchemedVersion =
    fromVersionAndScheme(CVersion(version), scheme)

  /** Helper function because it is awkward to handle the fact that
    * [[coursier.version.VersionCompatibility]] has two representations for
    * PVP.
    */
  private def isPVP(value: VersionCompatibility): Boolean =
    value match {
      case VersionCompatibility.PackVer =>
        true
      case VersionCompatibility.Default =>
        true
      case _ =>
        false
    }

}
