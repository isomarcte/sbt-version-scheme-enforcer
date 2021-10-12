package io.isomarcte.sbt.version.scheme.enforcer.core.vcs

import io.isomarcte.sbt.version.scheme.enforcer.core._
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

/** A data type which represents a Version Control System (VCS) tag.
  *
  * @note This is distinct from the concept of a "Version". A Tag is a concept
  *       which is explicitly linked to the use of a VCS system. Though
  *       uncommon and not normally a good idea, you can have a project with
  *       versions and no VCS, thus no tags.
  */
sealed abstract class Tag[A] extends Product with Serializable {

  def version: A

  /** The [[java.time.OffsetDateTime]] that the tag was created at.
    *
    * For example, if we choose our VCS system to be Git, then this is the
    * time you would see if you ran this command,
    *
    * {{{
    * > git --no-pager tag -v --format="%(creatordate:iso-strict)" v2.1.0.0
    * 2021-05-21T14:25:48-06:00
    * }}}
    */
  def creationDate: Option[OffsetDateTime]

  // Final //

  final def map[B](f: A => B): Tag[B] =
    Tag(f(version), creationDate)

  final def emap[B](f: A => Either[String, B]): Either[String, Tag[B]] =
    f(version).map(version => Tag(version, creationDate))

  final override def toString: String = s"Tag(version = ${version}, creationDate = ${creationDate})"
}

object Tag {

  // Backing implementation

  final private case class TagImpl[A](override val version: A, override val creationDate: Option[OffsetDateTime])
      extends Tag[A]

  // Public

  /** Create a new [[Tag]] from an arbitrary [[java.lang.String]] representing the
    * canonical name of the [[Tag]].
    */
  def apply[A](version: A): Tag[A] =
    TagImpl(version, None)

  /** Create a new [[Tag]] from an arbitrary [[java.lang.String]] representing the
    * canonical name of the [[Tag]] and a [[java.time.OffsetDateTime]]
    * representing the creation time of the [[Tag]].
    */
  def apply[A](version: A, creationDate: OffsetDateTime): Tag[A] = TagImpl(version, Some(creationDate))

  /** Create a new [[Tag]] from an arbitrary [[java.lang.String]] representing the
    * canonical name of the [[Tag]] and an optional [[java.time.OffsetDateTime]]
    * representing the creation time of the [[Tag]].
    */
  def apply[A](version: A, creationDate: Option[OffsetDateTime]): Tag[A] = TagImpl(version, creationDate)

  /** As [[#fromCreationDateString]], but always uses
    * [[java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME]] as the
    * [[java.time.format.DateTimeFormatter]].
    *
    * @note ISO 8601 is compatible with RFC 3339.
    *
    * @see [[https://datatracker.ietf.org/doc/html/rfc3339 RFC 3339]]
    */
  def fromCreationDateStringISO8601[A](version: A, creationDate: String): Either[Throwable, Tag[A]] =
    fromCreationDateString(version, creationDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

  /** Attempt to create a new [[Tag]] from a [[java.lang.String]] representing
    * the canonical name of the [[Tag]] and a [[java.lang.String]] which
    * should be able to be parsed as an [[OffsetDateTime]] using a given
    * [[DateTimeFormatter]].
    *
    * @note This method will ''fail'' if the parsing fails. If you merely wish
    *       to use [[None]] when parsing fails, you will have to do the
    *       parsing and then call one of the `apply` methods to create the
    *       value.
    */
  def fromCreationDateString[A](version: A, creationDate: String, format: DateTimeFormatter): Either[Throwable, Tag[A]] =
    Try(OffsetDateTime.parse(creationDate, format)).toEither.map(creationDate => apply(version, Some(creationDate)))

  def toPVP(tag: Tag[Version]): Either[String, Tag[PVPVersion]] =
    tag.emap(PVPVersion.fromVersion)

  implicit val order1Instance: Order1[Tag] =
    new Order1[Tag] {
      override def liftCompare[A, B](compare: A => B => Int, x: Tag[A], y: Tag[B]): Int =
        compare(x.version, y.version) match {
          case 0 =>
            Ordering[Option[OffsetDateTime]].compare(x.creationDate, y.creationDate)
          case otherwise =>
            otherwise
        }
    }

  implicit def orderingInstance[A: Ordering]: Ordering[Tag[A]] =
    Order1.orderingFromOrder1[Tag, A]

  def creationDateOrderingInstance[A: Ordering]: Ordering[Tag[A]] =
    new Ordering[Tag[A]] {
      override def compare(x: Tag[A], y: Tag[A]): Int =
        Ordering[Option[OffsetDateTime]].compare(x.creationDate, y.creationDate) match {
          case 0 =>
            Ordering[A].compare(x.version, y.version)
          case otherwise =>
            otherwise
        }
    }

  implicit def versionChangeTypeClassInstance[A](implicit A: VersionChangeTypeClass[A]): VersionChangeTypeClass[Tag[A]] =
    A.contramap(_.version)
}
