package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import munit._

final class SchemedVersionTests extends FunSuite {
  test("SchemedVersionTests compare") {
    val versionCompat = VersionCompatibility.apply("pvp").get
    val oldVersion    = SchemedVersion.fromVersionAndScheme(Version("1.9.0.0"), versionCompat)
    val newVersion    = SchemedVersion.fromVersionAndScheme(Version("1.10.0.0"), versionCompat)
    assertEquals(oldVersion.compare(newVersion), -1)

    assertEquals(newVersion.compare(oldVersion), 1)

    val three = SchemedVersion.fromVersionAndScheme(Version("1.0.0"), versionCompat)
    val four  = SchemedVersion.fromVersionAndScheme(Version("1.0.0.0"), versionCompat)

    assertEquals(four.compare(three), 2)

    assertEquals(three.compare(four), -2)
  }
}
