package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.toSortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.emapSortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet

sealed abstract class ProjectVersionInfo[A] extends Product with Serializable {
  protected implicit def orderingInstance: Ordering[A]

  def currentVersion: A
  def initialVersion: Option[A]
  def tags: Option[SortedSet[Tag[A]]]

  // final //

  final def map[B: Ordering](f: A => B): ProjectVersionInfo[B] =
    ProjectVersionInfo(f(currentVersion), initialVersion.map(f), tags.map(_.map(_.map(f))))

  final def emap[B: Ordering](f: A => Either[String, B]): Either[String, ProjectVersionInfo[B]] = {
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
    } yield ProjectVersionInfo(cv, iv, ts)
  }

  override final def toString: String =
    s"ProjectVersionInfo(currentVersion = ${currentVersion}, initialVersion = ${initialVersion}, tags = ${tags})"
}

object ProjectVersionInfo {
  private[this] final case class ProjectVersionInfoImpl[A](override val currentVersion: A, override val initialVersion: Option[A], override val tags: Option[SortedSet[Tag[A]]], orderingA: Ordering[A]) extends ProjectVersionInfo[A] {
    override protected implicit val orderingInstance: Ordering[A] = orderingA
  }

  def apply[A](currentVersion: A, initialVersion: Option[A], tags: Option[Set[Tag[A]]])(implicit A: Ordering[A]): ProjectVersionInfo[A] =
    ProjectVersionInfoImpl(currentVersion, initialVersion, tags.map(tags => toSortedSet[Tag[A]](tags)), A)
  def applyVersionScheme(versionScheme: VersionScheme, projectInfo: ProjectVersionInfo[Version]): Either[String, ProjectVersionInfo[versionScheme.VersionType]] = {
    implicit val versionTypeOrderingInstance: Ordering[versionScheme.VersionType] =
      versionScheme.versionTypeOrderingInstance
    projectInfo.emap((version: Version) =>
      versionScheme.fromVersion(version)
    )
  }

  def applyVersionSchemeSplitTags(versionScheme: VersionScheme, projectInfo: ProjectVersionInfo[Version]): Either[String, (ProjectVersionInfo[versionScheme.VersionType], Option[SortedSet[Tag[Version]]])] = {
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
    } yield (ProjectVersionInfo(cv, iv, splitTags.map(_._1)), splitTags.map(_._2))
  }

  implicit def orderingInstance[A: Ordering]: Ordering[ProjectVersionInfo[A]] =
    new Ordering[ProjectVersionInfo[A]] {
      override def compare(x: ProjectVersionInfo[A], y: ProjectVersionInfo[A]): Int =
        Ordering[A].compare(x.currentVersion, y.currentVersion) match {
          case 0 =>
            Ordering[Option[A]].compare(x.initialVersion, y.initialVersion)
          case otherwise =>
            otherwise
        }
    }
}
