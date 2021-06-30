package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._

/** Commonly used filters for [[Keys#versionSchemeEnforcerPreviousTagFilter]].
  */
object TagFilters {

  /** Filter which drops tags which represent a milestone release,
    * e.g. 1.1.0.0-M1.
    */
  val noMilestoneTagFilter: Tag => Boolean = { (value: Tag) =>
    if (value.value.matches(""".*-M\d+$""")) {
      false
    } else {
      true
    }
  }

  /** Filter which drops tags which represent a milestone release,
    * e.g. 1.1.0.0-M1.
    */
  @deprecated(message = "Please use noMilestoneTagFilter instead.", since = "2.1.1.0")
  lazy val noMilestoneFilter: String => Boolean = { (value: String) =>
    noMilestoneTagFilter(Tag(value))
  }
}
