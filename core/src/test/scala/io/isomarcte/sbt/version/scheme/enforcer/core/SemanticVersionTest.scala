package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class SemanticVersionTest extends FunSuite {

  // See https://semver.org/ section 11.
  test("SemanticVersion Ordering") {
    assert(SemanticVersion.unsafeFrom(BigInt(0), BigInt(0), BigInt(0)) < SemanticVersion.unsafeFrom(BigInt(0), BigInt(0), BigInt(1)))
    assert(SemanticVersion.unsafeFrom(BigInt(0), BigInt(0), BigInt(0)) < SemanticVersion.unsafeFrom(BigInt(0), BigInt(1), BigInt(0)))
    assert(SemanticVersion.unsafeFrom(BigInt(0), BigInt(0), BigInt(0)) < SemanticVersion.unsafeFrom(BigInt(1), BigInt(0), BigInt(0)))

    // Anything with a PreRelease is < anything without a PreRelease according to semver.
    assert(SemanticVersion.unsafeFrom(BigInt(0), BigInt(0), BigInt(0), Vector(VersionComponent.PreRelease.unsafeFromString("1"))) < SemanticVersion.unsafeFrom(BigInt(0), BigInt(0), BigInt(0)))

    assert(SemanticVersion.unsafeFrom(BigInt(0), BigInt(0), BigInt(0), Vector(VersionComponent.PreRelease.unsafeFromString("1"))) < SemanticVersion.unsafeFrom(BigInt(0), BigInt(0), BigInt(0), Vector(VersionComponent.PreRelease.unsafeFromString("2"))))
  }
}
