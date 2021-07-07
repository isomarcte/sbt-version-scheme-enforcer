package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class NumericSectionTests extends FunSuite {
  test("Construction validation") {
    assert(NumericSection.fromString("").isRight)
    assertEquals(NumericSection.fromString(""), Right(NumericSection.empty))
    assert(NumericSection.fromString(".").isLeft)
    assert(NumericSection.fromString("1.").isLeft)
    assert(NumericSection.fromString(".1.").isLeft)
    assert(NumericSection.fromString(".1").isLeft)

    assert(NumericSection.fromString("1.0.0").isRight)
    assert(NumericSection.fromString("1..0").isLeft)

    assert(NumericSection.fromString("1.0.0-RC1+01").isLeft)
  }

  test("Component parsing") {
    assertEquals(NumericSection.fromString("1.0.0").map(_.value.size), Right(3))
    assertEquals(NumericSection.fromString("").map(_.value.size), Right(0))
    assertEquals(NumericSection.fromString("1.0.0").map(_.value), Right(Vector(NumericVersionToken.unsafeFromBigInt(BigInt(1)), NumericVersionToken.unsafeFromBigInt(BigInt(0)), NumericVersionToken.unsafeFromBigInt(BigInt(0)))))
  }
}
