package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class NumericSectionTests extends FunSuite {
  test("Construction validation") {
    assert(NumericSection.fromString("").isRight)
    assert(NumericSection.fromString(".").isLeft)
    assert(NumericSection.fromString("1.").isLeft)
    assert(NumericSection.fromString(".1.").isLeft)
    assert(NumericSection.fromString(".1").isLeft)

    assert(NumericSection.fromString("1.0.0").isRight)
    assert(NumericSection.fromString("1..0").isLeft)

    assert(NumericSection.fromString("1.0.0-RC1+01").isLeft)
  }
}
