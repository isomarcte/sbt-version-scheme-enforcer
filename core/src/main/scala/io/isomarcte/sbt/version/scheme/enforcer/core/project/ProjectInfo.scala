package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.setToSortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.emapSortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet

sealed abstract class ProjectInfo[A] extends Product with Serializable {
  protected implicit def orderingInstance: Ordering[A]

  def currentVersion: A
  def initialVersion: Option[A]
  def tags: Option[SortedSet[Tag[A]]]

  // final //

  final def map[B: Ordering](f: A => B): ProjectInfo[B] =
    ProjectInfo(f(currentVersion), initialVersion.map(f), tags.map(_.map(_.map(f))))

  final def emap[B: Ordering](f: A => Either[String, B]): Either[String, ProjectInfo[B]] = {
    // poor man's traverse
    val ive: Either[String, Option[B]] =
      initialVersion.fold(
        Right(Option.empty): Either[String, Option[B]]
      )(a =>
        f(a).map(Option.apply _)
      )
    val tse: Either[String, Option[SortedSet[Tag[B]]]] =
      tags.fold(
        Right(None): Either[String, Option[SortedSet[Tag[B]]]]
      )(value =>
        emapSortedSet((t: Tag[A]) => t.emap(f))(value).map(Some(_))
      )
    for {
      cv <- f(currentVersion)
      iv <- ive
      ts <- tse
    } yield ProjectInfo(cv, iv, ts)
  }

  override final def toString: String =
    s"ProjectInfo(currentVersion = ${currentVersion}, initialVersion = ${initialVersion}, tags = ${tags})"
}

object ProjectInfo {
  private[this] final case class ProjectInfoImpl[A](override val currentVersion: A, override val initialVersion: Option[A], override val tags: Option[SortedSet[Tag[A]]], orderingA: Ordering[A]) extends ProjectInfo[A] {
    override protected implicit val orderingInstance: Ordering[A] = orderingA
  }

  def apply[A](currentVersion: A, initialVersion: Option[A], tags: Option[Set[Tag[A]]])(implicit A: Ordering[A]): ProjectInfo[A] =
    ProjectInfoImpl(currentVersion, initialVersion, tags.map(tags => setToSortedSet[Tag[A]](tags)), A)
  def applyVersionScheme(versionScheme: VersionScheme, projectInfo: ProjectInfo[Version]): Either[String, ProjectInfo[versionScheme.VersionType]] = {
    implicit val versionTypeOrderingInstance: Ordering[versionScheme.VersionType] =
      versionScheme.versionTypeOrderingInstance
    projectInfo.emap((version: Version) =>
      versionScheme.fromVersion(version)
    )
  }

  def applyVersionSchemeSplitTags(versionScheme: VersionScheme, projectInfo: ProjectInfo[Version]): Either[String, (ProjectInfo[versionScheme.VersionType], Option[SortedSet[Tag[Version]]])] = {
    implicit val versionTypeOrderingInstance: Ordering[versionScheme.VersionType] =
      versionScheme.versionTypeOrderingInstance
    lazy val splitTags: Option[(SortedSet[Tag[versionScheme.VersionType]], SortedSet[Tag[Version]])] =
      projectInfo.tags.map(_.foldLeft((SortedSet.empty, SortedSet.empty): (SortedSet[Tag[versionScheme.VersionType]], SortedSet[Tag[Version]])){
        case ((valid, invalid), value) =>
          value.emap(t => versionScheme.fromVersion(t)).fold(
            _ => (valid, invalid ++ SortedSet(value)),
            value => (valid ++ SortedSet(value), invalid)
          )
      }
      )

    for {
      cv <- versionScheme.fromVersion(projectInfo.currentVersion)
      iv <- projectInfo.initialVersion.fold(Right(None): Either[String, Option[versionScheme.VersionType]])(value => versionScheme.fromVersion(value).map(Some(_)))
    } yield (ProjectInfo(cv, iv, splitTags.map(_._1)), splitTags.map(_._2))
  }

  implicit def orderingInstance[A: Ordering]: Ordering[ProjectInfo[A]] =
    new Ordering[ProjectInfo[A]] {
      override def compare(x: ProjectInfo[A], y: ProjectInfo[A]): Int =
        Ordering[A].compare(x.currentVersion, y.currentVersion) match {
          case 0 =>
            Ordering[Option[A]].compare(x.initialVersion, y.initialVersion)
          case otherwise =>
            otherwise
        }
    }
}
