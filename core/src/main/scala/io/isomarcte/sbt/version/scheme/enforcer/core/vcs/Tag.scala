package io.isomarcte.sbt.version.scheme.enforcer.core.vcs

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
sealed abstract class Tag extends Ordered[Tag] {

  /** The [[java.lang.String]] representation of the canonical identifier of the
    * tag. The definition of a tag in all VCS systems ''must'' support this
    * data.
    *
    * For example, if we choose our VCS system to be Git, then this value is
    * what you would use to checkout a tag,
    *
    * {{{
    * $ git checkout v1.0.0.0 # v1.0.0.0 is the tag here.
    * }}}
    */
  def value: String

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

  final def withValue(newValue: String): Tag = Tag(newValue, creationDate)

  final def withCreationDate(newCreationDate: Option[OffsetDateTime]): Tag = Tag(value, newCreationDate)

  final override def toString: String = s"Tag(value = ${value}, creationDate = ${creationDate})"

  final override def compare(that: Tag): Int =
    Ordering[Option[OffsetDateTime]].compare(creationDate, that.creationDate) match {
      case 0 =>
        value.compare(that.value)
      case otherwise =>
        otherwise
    }
}

object Tag {

  // Backing implementation

  final private case class TagImpl(override val value: String, override val creationDate: Option[OffsetDateTime])
      extends Tag

  // Public

  /** Create a new [[Tag]] from an arbitrary [[java.lang.String]] representing the
    * canonical name of the [[Tag]].
    */
  def apply(value: String): Tag = apply(value, None)

  /** Create a new [[Tag]] from an arbitrary [[java.lang.String]] representing the
    * canonical name of the [[Tag]] and a [[java.time.OffsetDateTime]]
    * representing the creation time of the [[Tag]].
    */
  def apply(value: String, creationDate: OffsetDateTime): Tag = apply(value, Some(creationDate))

  /** Create a new [[Tag]] from an arbitrary [[java.lang.String]] representing the
    * canonical name of the [[Tag]] and an optional [[java.time.OffsetDateTime]]
    * representing the creation time of the [[Tag]].
    */
  def apply(value: String, creationDate: Option[OffsetDateTime]): Tag = TagImpl(value, creationDate)

  /** As [[#fromCreationDateString]], but always uses
    * [[java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME]] as the
    * [[java.time.format.DateTimeFormatter]].
    *
    * @note ISO 8601 is compatible with RFC 3339.
    *
    * @see [[https://datatracker.ietf.org/doc/html/rfc3339 RFC 3339]]
    */
  def fromCreationDateStringISO8601(value: String, creationDate: String): Either[Throwable, Tag] =
    fromCreationDateString(value, creationDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

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
  def fromCreationDateString(value: String, creationDate: String, format: DateTimeFormatter): Either[Throwable, Tag] =
    Try(OffsetDateTime.parse(creationDate, format)).toEither.map(creationDate => apply(value, Some(creationDate)))
}
