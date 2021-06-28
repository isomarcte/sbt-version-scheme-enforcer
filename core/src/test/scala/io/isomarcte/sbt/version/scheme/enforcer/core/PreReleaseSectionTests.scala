package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class PreReleaseSectionTests extends FunSuite {
  test("Construction validation") {
    assert(PreReleaseSection.fromString("").isLeft)
    assert(PreReleaseSection.fromString(".").isLeft)
    assert(PreReleaseSection.fromString("1.").isLeft)
    assert(PreReleaseSection.fromString(".1.").isLeft)
    assert(PreReleaseSection.fromString(".1").isLeft)
    assert(PreReleaseSection.fromString("1.0.0").isLeft)
    assert(PreReleaseSection.fromString("1..0").isLeft)
    assert(PreReleaseSection.fromString("1.0.0-RC1+01").isLeft)

    assert(PreReleaseSection.fromString("-").isRight)
    assertEquals(PreReleaseSection.fromString("-"), Right(PreReleaseSection.empty))

    assert(PreReleaseSection.fromString("--").isRight)
    assert(PreReleaseSection.fromString("-0.a-.B0").isRight)
    assert(PreReleaseSection.fromString("-0a").isRight)
    assert(PreReleaseSection.fromString("-00").isLeft)
    assert(PreReleaseSection.fromString("-.1").isLeft)
  }

  test("Component parsing") {
    assertEquals(PreReleaseSection.fromString("-SNAPSHOT"), Right(PreReleaseSection.snapshot))
    assertEquals(
      PreReleaseSection.fromString("-SNAPSHOT.1"),
      Right(
        PreReleaseSection(
          Vector(
            PreReleaseVersionToken.unsafeFromString("SNAPSHOT"),
            PreReleaseVersionToken.unsafeFromBigInt(BigInt(1))
          )
        )
      )
    )
  }
}
