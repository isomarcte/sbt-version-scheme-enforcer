package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import munit._

final class VersionChangeTypeTest extends FunSuite {

  test("Detect proper version change for EarlySemVer") {
    val f: (Version, Version) => Either[String, VersionChangeType] = VersionChangeType
      .fromPreviousAndNextVersion(VersionCompatibility.EarlySemVer)

    assertEquals(f(Version("0.0.0"), Version("0.0.1")), Right(VersionChangeType.Minor))

    assertEquals(f(Version("0.0.0"), Version("0.1.0")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.0.0"), Version("0.1.1")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.0.0"), Version("1.1.1")), Right(VersionChangeType.Major))

    assertEquals(f(Version("1.0.0"), Version("1.0.1")), Right(VersionChangeType.Patch))

    assertEquals(f(Version("1.0.0"), Version("1.1.1")), Right(VersionChangeType.Minor))

    assertEquals(f(Version("1.0.0"), Version("2.1.1")), Right(VersionChangeType.Major))
  }

  test("Detect proper version change for SemVerSpec") {
    val f: (Version, Version) => Either[String, VersionChangeType] = VersionChangeType
      .fromPreviousAndNextVersion(VersionCompatibility.SemVerSpec)

    assertEquals(f(Version("0.0.0"), Version("0.0.1")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.0.0"), Version("0.1.0")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.0.0"), Version("0.1.1")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.0.0"), Version("1.1.1")), Right(VersionChangeType.Major))

    assertEquals(f(Version("1.0.0"), Version("1.0.1")), Right(VersionChangeType.Patch))

    assertEquals(f(Version("1.0.0"), Version("1.1.1")), Right(VersionChangeType.Minor))

    assertEquals(f(Version("1.0.0"), Version("2.1.1")), Right(VersionChangeType.Major))
  }

  test("Detect proper version change for PackVer") {
    val f: (Version, Version) => Either[String, VersionChangeType] = VersionChangeType
      .fromPreviousAndNextVersion(VersionCompatibility.PackVer)

    assertEquals(f(Version("0.0.0"), Version("0.0.0.0")), Right(VersionChangeType.Patch))

    assertEquals(f(Version("0.0.0.0"), Version("0.0.0.1")), Right(VersionChangeType.Patch))

    assertEquals(f(Version("0.0.0.0"), Version("0.0.1.1")), Right(VersionChangeType.Minor))

    assertEquals(f(Version("0.0.0"), Version("0.0.1.1")), Right(VersionChangeType.Minor))

    assertEquals(f(Version("0.0.0"), Version("0.0.1")), Right(VersionChangeType.Minor))

    assertEquals(f(Version("0.0.0"), Version("0.1.0")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.0.0"), Version("0.1.0.0")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.0.0"), Version("1.1.0")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.0.0"), Version("1.1.0.0")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.1.0"), Version("1.1.0.0")), Right(VersionChangeType.Major))

    assertEquals(f(Version("0.1.0"), Version("2.1.0")), Right(VersionChangeType.Major))
  }
}
