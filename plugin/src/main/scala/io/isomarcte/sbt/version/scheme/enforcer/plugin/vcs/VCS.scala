package io.isomarcte.sbt.version.scheme.enforcer.plugin.vcs

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.vcs
import scala.collection.immutable.SortedSet
import scala.sys.process.ProcessLogger

/** Algebraic Data Type (ADT) for operating on various Version Control Systems
  * (VCS).
  *
  * @define outerError The outer `Either`'s left value indicates an error
  *                    reading the raw tag input from
  *                    [[#previousTagStringsWithError]].
  */
sealed private[plugin] trait VCS extends Product with Serializable {

  /** A simple [[java.lang.String]] representation of this VCS.
    */
  def asString: String

  /** All the previous VCS tags reachable from this commit or an error if
    * attempting to read the tags failed.
    *
    * @note This result may have some order (defined by the implementation)
    *       which is significant.
    */
  def previousTagStringsWithError: Either[Throwable, Vector[String]]

  /** An implementation dependent function to convert a particular VCS system's
    * textual representation of a tag into a [[Tag]] data type.
    */
  def tagStringToTag(value: String): Either[String, Tag]

  // final

  /** All of the tags or the parse errors from attempt to read a tag.
    *
    * @note $outerError
    */
  final def tagsWithErrors: Either[Throwable, Vector[Either[String, Tag]]] =
    previousTagStringsWithError.map(
      _.foldLeft(Vector.empty[Either[String, Tag]]) { case (acc, value) =>
        acc ++ Vector(tagStringToTag(value))
      }
    )

  /** All of the tags as a sorted set.
    *
    * @note $outerError
    */
  final def tags: Either[Throwable, SortedSet[Tag]] =
    tagsWithErrors.map(
      _.foldLeft(SortedSet.empty[Tag]) { case (acc, value) =>
        value.fold(Function.const(acc), value => acc ++ SortedSet(value))
      }
    )

  /** As [[tags]], but allows for transforming [[Tag]].
    *
    * @param transform A transformation function applied to the
    *        [[Tag]]. Returning `None` will cause the tag to be discarded,
    *        returning `Some(value)` will cause it to be
    *        transformed/replaced. This is a generic type of transform which
    *        can be used for filtering and transformation.
    *
    * @note $outerError
    */
  final def previousTagVersionsTransform(transform: Tag => Option[Tag]): Either[Throwable, SortedSet[Tag]] =
    tags.map(
      _.flatMap { value =>
        transform(value).fold(SortedSet.empty[Tag])(value => SortedSet(value))
      }
    )

  /** As [[#previousTagVersionsTransform]], but specialized to the common use
    * case of merely filtering out some tags.
    *
    * @note $outerError
    */
  final def previousTagVersionsFiltered(tagFilter: Tag => Boolean): Either[Throwable, SortedSet[Tag]] =
    previousTagVersionsTransform(value =>
      if (tagFilter(value)) {
        Some(value)
      } else {
        None
      }
    )
}

private[plugin] object VCS {

  /** All supported VCS types. */
  lazy val values: Set[VCS] = Set(Git)

  case object Git extends VCS {
    override val asString: String = "git"

    override lazy val previousTagStringsWithError: Either[Throwable, Vector[String]] = vcs.Git.previousGitTagStrings

    override def tagStringToTag(value: String): Either[String, Tag] =
      value.trim.split(' ').toList match {
        case tagString :: date :: Nil =>
          Tag
            .fromCreationDateStringISO8601(tagString, date)
            .fold(e => Left(e.getLocalizedMessage): Either[String, Tag], value => Right(value))
        case _ =>
          Left(s"Expected two tag components, '%(refname:strip=2) %(creatordate:iso-strict)', but got ${value}")
      }
  }

  /** Determine if the current project is using a supported [[VCS]]. Return an
    * error in the left project if it is not.
    */
  lazy val determineVCSE: Either[Throwable, VCS] =
    if (vcs.Git.isProjectUsingGit) {
      Right(Git)
    } else {
      Left(
        new RuntimeException(
          s"No supported VCS detected. Currently supported VCS systems are: ${values}. If the VCS you are using is not in this list and supports some concept of tagging, please make an issue at https://github.com/isomarcte/sbt-version-scheme-enforcer and we will endeavor to add support."
        )
      )
    }

  /** As [[#determineVCSE]], but returns an Option. */
  lazy val determineVCSOption: Option[VCS] = determineVCSE.toOption

  /** Determine if the current project is using ''any'' known VCS.
    */
  lazy val isUsingVCS: Boolean = determineVCSOption.isDefined

  /** A `ProcessLogger` which doesn't log any output.
    *
    * This can be used when checking if the project is a VCS system we
    * understand to avoid noisy command line warnings.
    */
  val silentProcessLogger: ProcessLogger = ProcessLogger(_ => ())
}
