package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class MetadataSectionTests extends FunSuite {
  test("Construction validation") {
    assert(MetadataSection.fromString("").isLeft)
    assert(MetadataSection.fromString(".").isLeft)
    assert(MetadataSection.fromString("1.").isLeft)
    assert(MetadataSection.fromString(".1.").isLeft)
    assert(MetadataSection.fromString(".1").isLeft)
    assert(MetadataSection.fromString("1.0.0").isLeft)
    assert(MetadataSection.fromString("1..0").isLeft)
    assert(MetadataSection.fromString("1.0.0-RC1+01").isLeft)

    assert(MetadataSection.fromString("+").isRight)
    assertEquals(MetadataSection.fromString("+"), Right(MetadataSection.empty))

    assert(MetadataSection.fromString("+-").isRight)
    assert(MetadataSection.fromString("--").isLeft)
    assert(MetadataSection.fromString("+0.a-.B0").isRight)
    assert(MetadataSection.fromString("+0a").isRight)
    assert(MetadataSection.fromString("+00").isRight)
    assert(MetadataSection.fromString("+.1").isLeft)
  }

  test("Component parsing") {
    assertEquals(MetadataSection.fromString("+1970.01.01"), Right(MetadataSection(Vector(MetadataVersionToken.unsafeFromString("1970"), MetadataVersionToken.unsafeFromString("01"), MetadataVersionToken.unsafeFromString("01")))))
  }
}
