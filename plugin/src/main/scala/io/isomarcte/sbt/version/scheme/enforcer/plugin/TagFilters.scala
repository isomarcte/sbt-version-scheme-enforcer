package io.isomarcte.sbt.version.scheme.enforcer.plugin

import scala.collection.immutable.SortedSet
import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._

/** Commonly used filters for [[Keys#versionSchemeEnforcerPreviousTagFilter]].
  */
object TagFilters {

  private[this] val milestoneRegex: String = """.*-M\d+$"""

  def nearestChangeType(
    changeTypes: NonEmptySet[VersionChangeType]
  ): VersionScheme => String => SortedSet[Tag] => SortedSet[String] = { (versionScheme: VersionScheme) => (currentVersion: String) => (tags: SortedSet[Tag]) =>
    tags.foldLeft(SortedSet.empty[String]){

    }
  }

  /** Filter which drops tags which represent a milestone release,
    * e.g. 1.1.0.0-M1.
    */
  @deprecated(message = "Please use noMilestoneTagFilter instead.", since = "2.1.1.0")
  lazy val noMilestoneFilter: String => Boolean = { (value: String) =>
    if (value.matches(milestoneRegex)) {
      false
    } else {
      true
    }
  }
}
