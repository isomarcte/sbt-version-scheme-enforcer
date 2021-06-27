package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._
import munit._

final class TagFilterTest extends FunSuite {

  test("noMilestoneFilter") {
    assert(TagFilters.noMilestoneTagFilter(Tag("1.0.0")))
    assert(TagFilters.noMilestoneTagFilter(Tag("1.0.0-M1")) === false)
  }
}
