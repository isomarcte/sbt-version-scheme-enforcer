package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import coursier.version.{Version => CVersion}
import munit._

final class VersionChangeTypeTest extends FunSuite {

  test("Detect proper version change for EarlySemVer") {
    val f: (CVersion, CVersion) => Either[String, VersionChangeType] = VersionChangeType
      .fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)

    assertEquals(f(CVersion("0.0.0"), CVersion("0.0.1")), Right(VersionChangeType.Minor))

    assertEquals(f(CVersion("0.0.0"), CVersion("0.1.0")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.0.0"), CVersion("0.1.1")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.0.0"), CVersion("1.1.1")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("1.0.0"), CVersion("1.0.1")), Right(VersionChangeType.Patch))

    assertEquals(f(CVersion("1.0.0"), CVersion("1.1.1")), Right(VersionChangeType.Minor))

    assertEquals(f(CVersion("1.0.0"), CVersion("2.1.1")), Right(VersionChangeType.Major))
  }

  test("Detect proper version change for SemVerSpec") {
    val f: (CVersion, CVersion) => Either[String, VersionChangeType] = VersionChangeType
      .fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)

    assertEquals(f(CVersion("0.0.0"), CVersion("0.0.1")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.0.0"), CVersion("0.1.0")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.0.0"), CVersion("0.1.1")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.0.0"), CVersion("1.1.1")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("1.0.0"), CVersion("1.0.1")), Right(VersionChangeType.Patch))

    assertEquals(f(CVersion("1.0.0"), CVersion("1.1.1")), Right(VersionChangeType.Minor))

    assertEquals(f(CVersion("1.0.0"), CVersion("2.1.1")), Right(VersionChangeType.Major))
  }

  test("Detect proper version change for PackVer") {
    val f: (CVersion, CVersion) => Either[String, VersionChangeType] = VersionChangeType
      .fromPreviousAndNextVersion(VersionCompatibility.PackVer)

    assertEquals(f(CVersion("0.0.0"), CVersion("0.0.0.0")), Right(VersionChangeType.Patch))

    assertEquals(f(CVersion("0.0.0.0"), CVersion("0.0.0.1")), Right(VersionChangeType.Patch))

    assertEquals(f(CVersion("0.0.0.0"), CVersion("0.0.1.1")), Right(VersionChangeType.Minor))

    assertEquals(f(CVersion("0.0.0"), CVersion("0.0.1.1")), Right(VersionChangeType.Minor))

    assertEquals(f(CVersion("0.0.0"), CVersion("0.0.1")), Right(VersionChangeType.Minor))

    assertEquals(f(CVersion("0.0.0"), CVersion("0.1.0")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.0.0"), CVersion("0.1.0.0")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.0.0"), CVersion("1.1.0")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.0.0"), CVersion("1.1.0.0")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.1.0"), CVersion("1.1.0.0")), Right(VersionChangeType.Major))

    assertEquals(f(CVersion("0.1.0"), CVersion("2.1.0")), Right(VersionChangeType.Major))
  }
}
