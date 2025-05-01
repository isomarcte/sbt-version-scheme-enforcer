package io.isomarcte.sbt.version.scheme.enforcer.plugin.vcs

import _root_.io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import io.isomarcte.sbt.version.scheme.enforcer.plugin.TagDomain
import scala.util.Success
import scala.util.Try

/** Functions for working with the Git VCS system.
  *
  * These primarily support the [[VCS]] type and related functions. They are
  * in a separate object to keep the size of the objects/files in the this
  * project easy for humans to read.
  */
private[plugin] object Git {

  /** Attempt to determine if the current project is using Git for VCS.
    */
  lazy val isProjectUsingGit: Boolean =
    Try(
      sys
        .process
        .Process(Seq("git", "--no-pager", "rev-parse", "--is-inside-work-tree"))
        .lineStream(VCS.silentProcessLogger)
        .headOption
        .map(_.trim.toLowerCase)
    ) match {
      case Success(Some(value)) =>
        value === "true"
      case _ =>
        false
    }

  /** If the current project is using Git for VCS, get a `Stream` of all the
    * previous tags reachable from this commit.
    */
  def gitTagStrings(domain: TagDomain): Either[Throwable, Vector[String]] =
    Try(
      sys
        .process
        .Process(
          Seq(
            "git",
            "--no-pager",
            "tag",
            "--format=%(refname:strip=2) %(creatordate:iso-strict)",
            "--sort=-creatordate",
            "--merged"
          ) ++ domainToArgSeq(domain)
        )
        .lineStream(VCS.silentProcessLogger)
        .toVector
    ).toEither

  /** Translate the [[TagDomain]] in to a `Seq` arguments for the `git` command
    * line invocation.
    */
  private[this] def domainToArgSeq(domain: TagDomain): Seq[String] =
    domain match {
      case TagDomain.All =>
        Seq.empty[String]
      case TagDomain.Reachable =>
        Seq("--merged", "@")
      case TagDomain.Unreachable =>
        Seq("--no-merged", "@")
      case TagDomain.Contains =>
        Seq("--contains", "@")
      case TagDomain.NoContains =>
        Seq("--no-contains", "@")
    }
}
