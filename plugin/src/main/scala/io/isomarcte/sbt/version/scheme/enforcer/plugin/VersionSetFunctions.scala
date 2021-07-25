package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core.project._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

object VersionSetFunctions {
  private type VersionSetF[A] = ProjectInfo[A] => Either[String, BinaryCheckInfo[A]]

  // def bimap[A: Ordering, B: Ordering](f: A => Either[String, B], g: B => A)(versionSetF: VersionSetF[B]): VersionSetF[A] =
  //   (projectInfo: ProjectInfo[A]) => (tagSet: SortedSet[Tag[A]]) => {
  //     projectInfo.emap[B](f).fold(
  //       Function.const(SortedSet.empty[A]),
  //       projectInfo => {
  //         val newTagSet: SortedSet[Tag[B]] =
  //           tagSet.foldLeft(SortedSet.empty[Tag[B]]){
  //             case (acc, value) =>
  //               acc ++ value.emap[B](f).fold(Function.const(SortedSet.empty[Tag[B]]), value => SortedSet(value))
  //           }
  //         versionSetF(projectInfo)(newTagSet).map(g)
  //       }
  //     )
  //   }

  val all: VersionSetF[Version] =
    (projectInfo: ProjectInfo[Version]) => {
      VersionScheme.
    }

  // def closestByChange(versionScheme: VersionScheme, targetChangeType: VersionChangeType): VersionSetF[Version] =
  //   (projectInfo: ProjectInfo[Version]) => (tagSet: SortedSet[Tag[Version]]) => {
  //     tagSet.foldLeft(BinaryCheckInfo.empty[Version]){
  //       case (acc, value) =>
  //         VersionScheme.changeType(projectInfo.currentVersion, value.version).flatMap((changeType: VersionChangeType) =>
  //           if (changeType === targetChangeType) {

  //           }
  //         )
  //     }
  //   }

  // def closestByChange(versionScheme: VersionScheme, targetChangeType: VersionChangeType): VersionSetF[String] =
  //   (projectInfo: ProjectInfo[Version]) => (tagSet: SortedSet[Tag[Version]]) => {
  //     tagSet.foldLeft(Option.empty[Either[String, Version]]){
  //       case (acc, value) =>
  //         acc.fold(
  //           VersionScheme.validateByScheme(versionScheme)(value.version)
  //         )((acc: Either[String, Version]) =>
  //           acc.fold(

  //           )
  //         )

  //         VersionScheme.changeType(versionScheme)(value.version, projectInfo.currentVersion).flatMap(changeType =>
  //           if (targetChangeType === changeType) {
  //             acc.fold(
  //               VersionScheme.validateByScheme(versionScheme)(value.version)
  //             )((acc: Either[String, Version]) =>
  //               acc.flatMap((acc: Version) =>
  //                 VersionScheme.orderByScheme(versionScheme)(acc, value.version).map{
  //                   case result if result <= 0 =>
  //                     value.version
  //                   case _ =>
  //                     acc
  //                 }
  //               )
  //             )
  //           } else {
  //           }
  //         )
  //     }
  //   }
}
