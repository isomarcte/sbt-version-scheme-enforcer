package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class PVPVersionTests extends FunSuite {
  test("Ordering") {
    val ordering: Ordering[PVPVersion] = Ordering[PVPVersion]
    import ordering.mkOrderingOps

    assert(PVPVersion.unsafeFromString("1.0.0-SNAPSHOT") < PVPVersion.unsafeFromString("1.0.0"))
    assert(PVPVersion.unsafeFromString("1.0.0-RC.1") < PVPVersion.unsafeFromString("1.0.0"))
    assert(PVPVersion.unsafeFromString("0.0.0") < PVPVersion.unsafeFromString("1.0.0"))
    assert(PVPVersion.unsafeFromString("0.1.0") < PVPVersion.unsafeFromString("1.0.0"))
    assert(PVPVersion.unsafeFromString("0.0.1") < PVPVersion.unsafeFromString("1.0.0"))

    assert(PVPVersion.unsafeFromString("1.0.0-RC.1+00000") < PVPVersion.unsafeFromString("1.0.0"))
    assert(PVPVersion.unsafeFromString("1.0.0-RC.1+00000") < PVPVersion.unsafeFromString("1.0.0-RC.1+00001"))
    assert(PVPVersion.unsafeFromString("1.0.0-RC.1+00000") < PVPVersion.unsafeFromString("1.0.0-RC.1+000000"))
    assert(PVPVersion.unsafeFromString("1.0.0-RC+00000") < PVPVersion.unsafeFromString("1.0.0-RC.1+0"))

    assert(PVPVersion.unsafeFromString("1.0.0") < PVPVersion.unsafeFromString("1.0.0.0"))
    assert(PVPVersion.unsafeFromString("1.0.0") < PVPVersion.unsafeFromString("1.0.0.0-SNAPSHOT"))
  }

  test("Components") {
    val version: PVPVersion = PVPVersion.unsafeFromString("1.2.3.4.5-SNAPSHOT.1+0.1")

    assertEquals(version.a, Some(NumericVersionToken.unsafeFromInt(1)))
    assertEquals(version.b, Some(NumericVersionToken.unsafeFromInt(2)))
    assertEquals(version.c, Some(NumericVersionToken.unsafeFromInt(3)))
    assertEquals(version.major, Vector(NumericVersionToken.unsafeFromInt(1), NumericVersionToken.unsafeFromInt(2)))
    assertEquals(version.minor, version.c)
    assertEquals(version.patch, Vector(NumericVersionToken.unsafeFromInt(4), NumericVersionToken.unsafeFromInt(5)))
    assert(version.isPreRelease)
    assertEquals(version.preReleaseSection, Some(PreReleaseSection.unsafeFromString("-SNAPSHOT.1")))
    assertEquals(version.metadataSection, Some(MetadataSection.unsafeFromString("+0.1")))
  }
}
