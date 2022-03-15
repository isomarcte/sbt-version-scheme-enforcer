package io.isomarcte.sbt.version.scheme.enforcer.core.project

import io.isomarcte.sbt.version.scheme.enforcer.core.project.syntax.all._
import io.isomarcte.sbt.version.scheme.enforcer.core.internal._
import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.toSortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core.internal.emapSortedSet
import scala.collection.immutable.SortedSet

sealed abstract class ProjectVersionInfo[A] extends Product with Serializable {
  protected implicit def orderingInstance: Ordering[A]

  def currentVersion: A
  def initialVersion: Option[A]
  def previousVersions: Option[SortedSet[BinaryCheckVersion[A]]]

  // final //

  final def withPreviousVersions(value: Option[Set[BinaryCheckVersion[A]]]): ProjectVersionInfo[A] =
    ProjectVersionInfo(currentVersion, initialVersion, value.map(value => toSortedSet(value)))

  final def mapPreviousVersions(f: Option[Set[BinaryCheckVersion[A]]] => Option[Set[BinaryCheckVersion[A]]]): ProjectVersionInfo[A] =
    withPreviousVersions(f(previousVersions))

  final def map[B: Ordering](f: A => B): ProjectVersionInfo[B] =
    ProjectVersionInfo(f(currentVersion), initialVersion.map(f), previousVersions.map(_.map(_.map(f))))

  final def emap[B: Ordering](f: A => Either[String, B]): Either[String, ProjectVersionInfo[B]] = {
    // poor man's traverse
    val ive: Either[String, Option[B]] =
      initialVersion.fold(
        Right(Option.empty): Either[String, Option[B]]
      )(a =>
        f(a).map(Option.apply _)
      )
    val tse: Either[String, Option[SortedSet[BinaryCheckVersion[B]]]] =
      previousVersions.fold(
        Right(None): Either[String, Option[SortedSet[BinaryCheckVersion[B]]]]
      )(value =>
        emapSortedSet((t: BinaryCheckVersion[A]) => t.emap(f))(value).map(Some(_))
      )
    for {
      cv <- f(currentVersion)
      iv <- ive
      ts <- tse
    } yield ProjectVersionInfo(cv, iv, ts)
  }

  override final def toString: String =
    s"ProjectVersionInfo(currentVersion = ${currentVersion}, initialVersion = ${initialVersion}, previousVersions = ${previousVersions})"
}

object ProjectVersionInfo {
  private[this] final case class ProjectVersionInfoImpl[A](override val currentVersion: A, override val initialVersion: Option[A], override val previousVersions: Option[SortedSet[BinaryCheckVersion[A]]], orderingA: Ordering[A]) extends ProjectVersionInfo[A] {
    override protected implicit val orderingInstance: Ordering[A] = orderingA
  }

  def apply[A](currentVersion: A, initialVersion: Option[A], previousVersions: Option[Set[BinaryCheckVersion[A]]])(implicit A: Ordering[A]): ProjectVersionInfo[A] =
    ProjectVersionInfoImpl(currentVersion, initialVersion, previousVersions.map(previousVersions => toSortedSet[BinaryCheckVersion[A]](previousVersions)), A)

  def applyVersionScheme(versionScheme: VersionScheme, projectInfo: ProjectVersionInfo[Version]): Either[String, ProjectVersionInfo[versionScheme.VersionType]] = {
    implicit val versionTypeOrderingInstance: Ordering[versionScheme.VersionType] =
      versionScheme.versionTypeOrderingInstance
    projectInfo.emap((version: Version) =>
      versionScheme.fromVersion(version)
    )
  }

  // def applyVersionSchemeSplitTags[F[_], A](versionScheme: VersionScheme, projectInfo: ProjectVersionInfo[F[A]]): Either[String, (ProjectVersionInfo[F[versionScheme.VersionType]], Option[SortedSet[Tag[A]]])] = {
  //   implicit val versionTypeOrderingInstance: Ordering[versionScheme.VersionType] =
  //     versionScheme.versionTypeOrderingInstance
  //   lazy val splitTags: Option[(SortedSet[Tag[versionScheme.VersionType]], SortedSet[Tag[A]])] =
  //     projectInfo.tags.map(_.foldLeft((SortedSet.empty, SortedSet.empty): (SortedSet[Tag[versionScheme.VersionType]], SortedSet[Tag[Version]])){
  //       case ((valid, invalid), value) =>
  //         value.emap(t => versionScheme.fromVersion(t)).fold(
  //           _ => (valid, invalid ++ SortedSet(value)),
  //           value => (valid ++ SortedSet(value), invalid)
  //         )
  //     }
  //     )

  //   for {
  //     cv <- versionScheme.fromVersion(projectInfo.currentVersion)
  //     iv <- projectInfo.initialVersion.fold(Right(None): Either[String, Option[versionScheme.VersionType]])(value => versionScheme.fromVersion(value).map(Some(_)))
  //   } yield (ProjectVersionInfo(cv, iv, splitTags.map(_._1)), splitTags.map(_._2))
  // }

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

  implicit def versionSchemableClassInstance1[F[_], A](implicit F: VersionSchemableClass[F, A], G: Order1[F]): VersionSchemableClass[Lambda[B => ProjectVersionInfo[F[B]]], A] = {
    val sortedSetInstance = VersionSchemableClass.sortedSetInstance[Lambda[B => BinaryCheckVersion[F[B]]], A]
    new VersionSchemableClass[Lambda[B => ProjectVersionInfo[F[B]]], A] {
      override def scheme(versionScheme: VersionScheme, value: ProjectVersionInfo[F[A]]): Either[String, ProjectVersionInfo[F[versionScheme.VersionType]]] = {
        implicit val versionTypeOrderingF: Ordering[F[versionScheme.VersionType]] = Order1.orderingFromOrder1(G, versionScheme.versionTypeOrderingInstance)
        for {
          current <- F.scheme(versionScheme, value.currentVersion)
          initial <- value.initialVersion.fold(Right(None): Either[String, Option[F[versionScheme.VersionType]]])(value => F.scheme(versionScheme, value).map(value => Some(value)))
          previous <- value.previousVersions.fold(Right(None): Either[String, Option[SortedSet[BinaryCheckVersion[F[versionScheme.VersionType]]]]])(value => sortedSetInstance.scheme(versionScheme, value).map((value: SortedSet[BinaryCheckVersion[F[versionScheme.VersionType]]]) => Some(value)))
        } yield ProjectVersionInfo(current, initial, previous)
    }
    }
  }

  implicit def versionSchemableClassInstance[A: VersionSchemableClass.VersionChangeTypeClassId]: VersionSchemableClass[ProjectVersionInfo, A] =
    versionSchemableClassInstance1[Id, A]
}
