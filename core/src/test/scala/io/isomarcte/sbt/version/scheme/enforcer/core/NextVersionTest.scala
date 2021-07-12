package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import coursier.version.{Version => CVersion}
import munit._
import scala.annotation.nowarn

@nowarn("cat=deprecation")
final class NextVersionTest extends FunSuite {

  test("EarlySemVer Minimum Next Version") {
    val f: (VersionChangeType, CVersion) => Either[String, CVersion] = NextVersion
      .minimumNextVersion(VersionCompatibility.EarlySemVer)

    assertEquals(f(VersionChangeType.Patch, CVersion("0.0.0")), Right(CVersion("0.0.1")))

    assertEquals(f(VersionChangeType.Minor, CVersion("0.0.0")), Right(CVersion("0.0.1")))

    assertEquals(f(VersionChangeType.Major, CVersion("0.0.0")), Right(CVersion("0.1.0")))

    assertEquals(f(VersionChangeType.Patch, CVersion("1.0.0")), Right(CVersion("1.0.1")))

    assertEquals(f(VersionChangeType.Minor, CVersion("1.0.0")), Right(CVersion("1.1.0")))

    assertEquals(f(VersionChangeType.Major, CVersion("1.0.0")), Right(CVersion("2.0.0")))
  }

  test("SemVerSpec Minimum Next Version") {
    val f: (VersionChangeType, CVersion) => Either[String, CVersion] = NextVersion
      .minimumNextVersion(VersionCompatibility.SemVerSpec)

    assertEquals(f(VersionChangeType.Patch, CVersion("0.0.0")), Right(CVersion("0.0.1")))

    assertEquals(f(VersionChangeType.Minor, CVersion("0.0.0")), Right(CVersion("0.0.1")))

    assertEquals(f(VersionChangeType.Major, CVersion("0.0.0")), Right(CVersion("0.0.1")))

    assertEquals(f(VersionChangeType.Patch, CVersion("1.0.0")), Right(CVersion("1.0.1")))

    assertEquals(f(VersionChangeType.Minor, CVersion("1.0.0")), Right(CVersion("1.1.0")))

    assertEquals(f(VersionChangeType.Major, CVersion("1.0.0")), Right(CVersion("2.0.0")))
  }

  test("PackVer Minimum Next Version") {
    val f: (VersionChangeType, CVersion) => Either[String, CVersion] = NextVersion
      .minimumNextVersion(VersionCompatibility.PackVer)

    assertEquals(f(VersionChangeType.Patch, CVersion("0.0.0")), Right(CVersion("0.0.0.0")))

    assertEquals(f(VersionChangeType.Patch, CVersion("0.0.0.0")), Right(CVersion("0.0.0.1")))

    assertEquals(f(VersionChangeType.Minor, CVersion("0.0.0")), Right(CVersion("0.0.1")))

    assertEquals(f(VersionChangeType.Minor, CVersion("0.0.0.0")), Right(CVersion("0.0.1")))

    assertEquals(f(VersionChangeType.Major, CVersion("0.0.0")), Right(CVersion("0.1.0")))

    assertEquals(f(VersionChangeType.Major, CVersion("0.0.0.0")), Right(CVersion("0.1.0")))

    assertEquals(f(VersionChangeType.Major, CVersion("2.1.2.3")), Right(CVersion("2.2.0")))

    assertEquals(f(VersionChangeType.Major, CVersion("3.0.1")), Right(CVersion("3.1.0")))
  }
}
