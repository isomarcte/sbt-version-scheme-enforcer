package io.isomarcte.sbt.version.scheme.enforcer.plugin.vcs

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.plugin._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.vcs
import scala.sys.process.ProcessLogger

/** Algebraic Data Type (ADT) for operating on various Version Control Systems
  * (VCS).
  */
sealed private[plugin] trait VCS extends Product with Serializable {

  /** A simple [[java.lang.String]] representation of this VCS.
    */
  def asString: String

  /** All the VCS tags to consider for calculating the previous version.
    *
    * @param domain The [[TagDomain]] can be used to define a domain of tags
    *        to consider. For example, you can use [[TagDomain#All]] if you
    *        want to consider all tags in the repository or
    *        [[TagDomain#Reachable]] if you want to only consider tags which
    *        are reachable (usually ancestors) of this commit.
    */
  def tagStrings(domain: TagDomain): Stream[String]

  // final //

  /** As [[#tagStrings]], but the tags are parsed into
    * [[NumericVersion]] values. Non numeric versions are filtered from the
    * `Stream`.
    *
    * @param transform A transformation function applied to the
    *        [[java.lang.String]] representation of a tag. Returning `None`
    *        will cause the tag to be discarded, returning `Some(value)` will
    *        cause it to be transformed/replaced. This is a generic type of
    *        transform which can be used for filtering and transformation.
    */
  final def tagVersionsTransform(transform: String => Option[String], domain: TagDomain): Stream[NumericVersion] =
    tagStrings(domain)
      .flatMap { value =>
        transform(value).fold(Stream.empty[String])(value => Stream(value))
      }
      .flatMap(value =>
        NumericVersion.fromString(value).fold(Function.const(Stream.empty[NumericVersion]), value => Stream(value))
      )

  /** As [[#tagVersionsTransform]], but specialized to the common use
    * case of merely filtering out some tags. No transformation is done in on
    * the tag value itself other than converting it to a [[NumericVersion]].
    */
  final def tagVersionsFiltered(tagFilter: String => Boolean, domain: TagDomain): Stream[NumericVersion] =
    tagVersionsTransform(
      value =>
        if (tagFilter(value)) {
          Some(value)
        } else {
          None
        },
      domain
    )
}

private[plugin] object VCS {

  /** All supported VCS types. */
  lazy val values: Set[VCS] = Set(Git)

  case object Git extends VCS {
    override val asString: String = "git"

    override def tagStrings(domain: TagDomain): Stream[String] = vcs.Git.gitTagStrings(domain)
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
