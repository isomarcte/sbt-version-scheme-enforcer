package io.isomarcte.sbt.version.scheme.enforcer.plugin

/** Commonly used filters for [[Keys#versionSchemeEnforcerPreviousTagFilter]].
  */
object TagFilters {

  /** Filter which drops tags which represent a milestone release,
    * e.g. 1.1.0.0-M1.
    */
  val noMilestoneFilter: String => Boolean = { (value: String) =>
    if (value.matches(""".*-M\d+$""")) {
      false
    } else {
      true
    }
  }
}
