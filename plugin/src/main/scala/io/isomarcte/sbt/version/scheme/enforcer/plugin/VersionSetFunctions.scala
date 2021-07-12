package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core.project._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._

object VersionSetFunctions {
  private type VersionSetF[A] = ProjectInfo[A] => SortedSet[Tag[A]] => SortedSet[A]

  def bimap[A: Ordering, B: Ordering](f: A => Either[String, B], g: B => A)(versionSetF: VersionSetF[B]): VersionSetF[A] =
    (projectInfo: ProjectInfo[A]) => (tagSet: SortedSet[Tag[A]]) => {
      projectInfo.emap[B](f).fold(
        Function.const(SortedSet.empty[A]),
        projectInfo => {
          val newTagSet: SortedSet[Tag[B]] =
            tagSet.foldLeft(SortedSet.empty[Tag[B]]){
              case (acc, value) =>
                acc ++ value.emap[B](f).fold(Function.const(SortedSet.empty[Tag[B]]), value => SortedSet(value))
            }
          versionSetF(projectInfo)(newTagSet).map(g)
        }
      )
    }

  def closestMajorChange(versionScheme: VersionScheme): VersionSetF[String] =

}
