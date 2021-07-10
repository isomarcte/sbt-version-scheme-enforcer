package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class SemVerVersionTests extends FunSuite {
  test("Ordering") {
    val ordering: Ordering[SemVerVersion] = Ordering[SemVerVersion]
    import ordering.mkOrderingOps

    assert(SemVerVersion.unsafeFromString("1.0.0-SNAPSHOT") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("1.0.0-RC.1") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("0.0.0") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("0.1.0") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("0.0.1") < SemVerVersion.unsafeFromString("1.0.0"))

    assert(SemVerVersion.unsafeFromString("1.0.0-RC.1+00000") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("1.0.0-RC.1+00000") < SemVerVersion.unsafeFromString("1.0.0-RC.1+00001"))
    assert(SemVerVersion.unsafeFromString("1.0.0-RC.1+00000") < SemVerVersion.unsafeFromString("1.0.0-RC.1+000000"))
    assert(SemVerVersion.unsafeFromString("1.0.0-RC+00000") < SemVerVersion.unsafeFromString("1.0.0-RC.1+0"))
  }

  test("Precedence") {
    val ordering: Ordering[SemVerVersion] = SemVerVersion.semVerPrecedenceOrdering
    import ordering.mkOrderingOps

    assert(SemVerVersion.unsafeFromString("1.0.0-SNAPSHOT") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("1.0.0-RC.1") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("0.0.0") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("0.1.0") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("0.0.1") < SemVerVersion.unsafeFromString("1.0.0"))

    assert(SemVerVersion.unsafeFromString("1.0.0-RC.1+00000") < SemVerVersion.unsafeFromString("1.0.0"))
    assert(SemVerVersion.unsafeFromString("1.0.0-RC.1+00000").equiv(SemVerVersion.unsafeFromString("1.0.0-RC.1+00001")))
    assert(
      SemVerVersion.unsafeFromString("1.0.0-RC.1+00000").equiv(SemVerVersion.unsafeFromString("1.0.0-RC.1+000000"))
    )
    assert(SemVerVersion.unsafeFromString("1.0.0-RC+00000") < SemVerVersion.unsafeFromString("1.0.0-RC.1+0"))
  }

  test("VersionChangeTypeClass") {
    val instance: VersionChangeTypeClass[SemVerVersion] = VersionChangeTypeClass[SemVerVersion]

    assertEquals(
      instance.changeType(SemVerVersion.unsafeFromString("0.0.0"), SemVerVersion.unsafeFromString("0.0.0")),
      VersionChangeType.Patch
    )
    assertEquals(
      instance.changeType(SemVerVersion.unsafeFromString("0.0.0"), SemVerVersion.unsafeFromString("0.0.1")),
      VersionChangeType.Major
    )
    assertEquals(
      instance.changeType(SemVerVersion.unsafeFromString("0.0.0"), SemVerVersion.unsafeFromString("0.1.0")),
      VersionChangeType.Major
    )
    assertEquals(
      instance.changeType(SemVerVersion.unsafeFromString("0.0.0"), SemVerVersion.unsafeFromString("1.0.0")),
      VersionChangeType.Major
    )

    assertEquals(
      instance.changeType(SemVerVersion.unsafeFromString("1.0.0"), SemVerVersion.unsafeFromString("1.0.0")),
      VersionChangeType.Patch
    )
    assertEquals(
      instance.changeType(SemVerVersion.unsafeFromString("1.0.0"), SemVerVersion.unsafeFromString("1.0.1")),
      VersionChangeType.Patch
    )
    assertEquals(
      instance.changeType(SemVerVersion.unsafeFromString("1.0.0"), SemVerVersion.unsafeFromString("1.1.0")),
      VersionChangeType.Minor
    )
    assertEquals(
      instance.changeType(SemVerVersion.unsafeFromString("1.0.0"), SemVerVersion.unsafeFromString("2.0.0")),
      VersionChangeType.Major
    )
  }
}
