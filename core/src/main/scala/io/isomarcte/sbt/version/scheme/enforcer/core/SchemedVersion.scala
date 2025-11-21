package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import coursier.version.{Version => CVersion}
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import scala.util.Success
import scala.util.Try

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
        val thisChunks    = this.version.repr.split('.')
        val thatChunks    = that.version.repr.split('.')
        val stringCompare = this.version.repr.compareTo(that.version.repr)
        if (thisChunks.size === thatChunks.size)
          (thisChunks zip thatChunks)
            .map { case (first, second) =>
              Try(first.toInt.compareTo(second.toInt)) match {
                case Success(value) =>
                  value
                case _ =>
                  first.compareTo(second)
              }
            }
            .dropWhile(_ === 0)
            .headOption
            .getOrElse(0)
        else
          stringCompare
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
