package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class VersionSectionsTests extends FunSuite {
  test("Construction validation") {
    assert(VersionSections.fromString("").isRight)
    assert(VersionSections.fromString("1").isRight)
    assert(VersionSections.fromString("1.0").isRight)
    assert(VersionSections.fromString("1.0.0").isRight)
    assert(VersionSections.fromString("1.0.0.0").isRight)
    assert(VersionSections.fromString("1.0.0.0-SNAPSHOT").isRight)
    assert(VersionSections.fromString("1.0.0.0-").isRight)
    assert(VersionSections.fromString("1.0.0.0-+").isRight)
    assert(VersionSections.fromString("1.0.0.0+-").isRight)
    assert(VersionSections.fromString("1.0.0.0-SNAPSHOT+1970.01.01").isRight)

    assert(VersionSections.fromString("1.0.0-001").isLeft)
    assert(VersionSections.fromString("1.0.0-001a").isRight)
  }

  test("Ordering") {
    val ordering: Ordering[VersionSections] = Ordering[VersionSections]
    import ordering.mkOrderingOps

    assert(VersionSections.unsafeFromString("1.0.0") < VersionSections.unsafeFromString("1.0.0.0"))

    assert(VersionSections.unsafeFromString("1.0.0-SNAPSHOT") < VersionSections.unsafeFromString("1.0.0"))
    assert(VersionSections.unsafeFromString("1.0.0-RC0") < VersionSections.unsafeFromString("1.0.0-RC1"))
    assert(VersionSections.unsafeFromString("1.0.0-RC0") < VersionSections.unsafeFromString("1.0.0-RC0.1"))
    assert(VersionSections.unsafeFromString("1.0.0-RC0+9") < VersionSections.unsafeFromString("1.0.0-RC1"))
    assert(VersionSections.unsafeFromString("1.0.0-RC0+9") < VersionSections.unsafeFromString("1.0.0-RC0.1"))
  }
}
