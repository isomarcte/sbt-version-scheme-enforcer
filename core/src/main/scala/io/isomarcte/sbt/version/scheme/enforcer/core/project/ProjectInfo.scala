package io.isomarcte.sbt.version.scheme.enforcer.core.project

import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._

sealed abstract class ProjectInfo[A] extends Product with Serializable {
  def currentVersion: A
  def initialVersion: Option[A]
  def tags: SortedSet[Tag[A]]
  def versionSchemeString: String

  // final //

  final def map[B](f: A => B): ProjectInfo[B] =
    ProjectInfo(f(currentVersion), initialVersion.map(f))

  final def emap[B](f: A => Either[String, B]): Either[String, ProjectInfo[B]] = {
    // poor man's traverse
    val ive: Either[String, Option[B]] =
      initialVersion.fold(
        Right(Option.empty): Either[String, Option[B]]
      )(a =>
        f(a).map(Option.apply _)
      )
    for {
      cv <- f(currentVersion)
      iv <- ive
    } yield ProjectInfo(cv, iv)
  }

  override final def toString: String =
    s"ProjectInfo(currentVersion = ${currentVersion}, initialVersion = ${initialVersion})"
}

object ProjectInfo {
  // private[this] final case class ProjectInfoImpl[A](override val currentVersion: A, override val initialVersion: Option[A]) extends ProjectInfo[A]

  def apply[A](currentVersion: A, initialVersion: Option[A]): ProjectInfo[A] =
    ???
    // ProjectInfoImpl(currentVersion, initialVersion)

  def toPVP(projectInfo: ProjectInfo[Version]): Either[String, ProjectInfo[PVPVersion]] =
    projectInfo.emap(PVPVersion.fromVersion)

  def toSemVer(projectInfo: ProjectInfo[Version]): Either[String, ProjectInfo[SemVerVersion]] =
    projectInfo.emap(SemVerVersion.fromVersion)

  def toEarlySemVer(projectInfo: ProjectInfo[Version]): Either[String, ProjectInfo[EarlySemVerVersion]] =
    projectInfo.emap(EarlySemVerVersion.fromVersion)

  def toAlwaysVersion(projectInfo: ProjectInfo[Version]): ProjectInfo[AlwaysVersion] =
    projectInfo.map(AlwaysVersion.fromVersion)

  def toStrictVersion(projectInfo: ProjectInfo[Version]): ProjectInfo[StrictVersion] =
    projectInfo.map(StrictVersion.fromVersion)

  def toVersion(projectInfo: ProjectInfo[String]): ProjectInfo[Version] =
    projectInfo.map(Version.apply)

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
