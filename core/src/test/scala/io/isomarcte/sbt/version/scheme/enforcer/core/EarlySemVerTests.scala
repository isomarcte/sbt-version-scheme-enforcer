package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class EarlySemVerVersionTests extends FunSuite {
  test("VersionChangeTypeClass") {
    val instance: VersionChangeTypeClass[EarlySemVerVersion] = VersionChangeTypeClass[EarlySemVerVersion]

    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("0.0.0"), EarlySemVerVersion.unsafeFromString("0.0.0")), VersionChangeType.Patch)
    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("0.0.0"), EarlySemVerVersion.unsafeFromString("0.0.1")), VersionChangeType.Minor)
    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("0.0.0"), EarlySemVerVersion.unsafeFromString("0.1.0")), VersionChangeType.Major)
    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("0.0.0"), EarlySemVerVersion.unsafeFromString("1.0.0")), VersionChangeType.Major)
    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("0.1.0"), EarlySemVerVersion.unsafeFromString("1.0.0")), VersionChangeType.Major)

    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("1.0.0"), EarlySemVerVersion.unsafeFromString("1.0.0")), VersionChangeType.Patch)
    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("1.0.0"), EarlySemVerVersion.unsafeFromString("1.0.1")), VersionChangeType.Patch)
    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("1.0.0"), EarlySemVerVersion.unsafeFromString("1.1.0")), VersionChangeType.Minor)
    assertEquals(instance.changeType(EarlySemVerVersion.unsafeFromString("1.0.0"), EarlySemVerVersion.unsafeFromString("2.0.0")), VersionChangeType.Major)
  }
}
