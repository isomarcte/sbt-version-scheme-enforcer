package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.project._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import scala.collection.immutable.SortedSet

object VersionSetFunctions {
  private type VersionSetF[A] = ProjectInfo[A] => SortedSet[Tag[A]] => SortedSet[A]

  def from[A, B](f: A => Either[String, B])(g: VersionSetF[A]): VersionSetF[B] =
    (projectInfo: ProjectInfo[B]) => (tagSet: SortedSet[Tag[B]]) => {
      projectInfo.emap(f).fold(
        Function.const(Function.const(SortedSet.empty[B])),
        projectInfo => {
          val newTagSet: SortedSet[Tag[A]] =
            tagSet.foldLeft(SortedSet.empty[A]){
              case (acc, value) =>
                acc ++ value.emap(f).fold(Function.const(SortedSet.empty[A]), value => SortedSet(value))
            }
          g(projectInfo)(newTagSet)
        }
      )
    }
}
