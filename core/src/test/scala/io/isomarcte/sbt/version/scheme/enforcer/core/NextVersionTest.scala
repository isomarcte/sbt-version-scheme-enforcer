package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import munit._

final class NextVersionTest extends FunSuite {

  test("EarlySemVer Minimum Next Version") {
    val f: (VersionChangeType, Version) => Either[String, Version] = NextVersion
      .minimumNextVersion(VersionCompatibility.EarlySemVer)

    assertEquals(f(VersionChangeType.Patch, Version("0.0.0")), Right(Version("0.0.1")))

    assertEquals(f(VersionChangeType.Minor, Version("0.0.0")), Right(Version("0.0.1")))

    assertEquals(f(VersionChangeType.Major, Version("0.0.0")), Right(Version("0.1.0")))

    assertEquals(f(VersionChangeType.Patch, Version("1.0.0")), Right(Version("1.0.1")))

    assertEquals(f(VersionChangeType.Minor, Version("1.0.0")), Right(Version("1.1.0")))

    assertEquals(f(VersionChangeType.Major, Version("1.0.0")), Right(Version("2.0.0")))
  }

  test("SemVerSpec Minimum Next Version") {
    val f: (VersionChangeType, Version) => Either[String, Version] = NextVersion
      .minimumNextVersion(VersionCompatibility.SemVerSpec)

    assertEquals(f(VersionChangeType.Patch, Version("0.0.0")), Right(Version("0.0.1")))

    assertEquals(f(VersionChangeType.Minor, Version("0.0.0")), Right(Version("0.0.1")))

    assertEquals(f(VersionChangeType.Major, Version("0.0.0")), Right(Version("0.0.1")))

    assertEquals(f(VersionChangeType.Patch, Version("1.0.0")), Right(Version("1.0.1")))

    assertEquals(f(VersionChangeType.Minor, Version("1.0.0")), Right(Version("1.1.0")))

    assertEquals(f(VersionChangeType.Major, Version("1.0.0")), Right(Version("2.0.0")))
  }

  test("PackVer Minimum Next Version") {
    val f: (VersionChangeType, Version) => Either[String, Version] = NextVersion
      .minimumNextVersion(VersionCompatibility.PackVer)

    assertEquals(f(VersionChangeType.Patch, Version("0.0.0")), Right(Version("0.0.0.0")))

    assertEquals(f(VersionChangeType.Patch, Version("0.0.0.0")), Right(Version("0.0.0.1")))

    assertEquals(f(VersionChangeType.Minor, Version("0.0.0")), Right(Version("0.0.1")))

    assertEquals(f(VersionChangeType.Minor, Version("0.0.0.0")), Right(Version("0.0.1")))

    assertEquals(f(VersionChangeType.Major, Version("0.0.0")), Right(Version("0.1.0")))

    assertEquals(f(VersionChangeType.Major, Version("0.0.0.0")), Right(Version("0.1.0")))

    assertEquals(f(VersionChangeType.Major, Version("2.1.2.3")), Right(Version("2.2.0")))

    assertEquals(f(VersionChangeType.Major, Version("3.0.1")), Right(Version("3.1.0")))
  }
}
