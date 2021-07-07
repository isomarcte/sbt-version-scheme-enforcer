package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class PreReleaseVersionTokenTests extends FunSuite {
  test("Construction validation") {
    assert(PreReleaseVersionToken.fromString("00").isLeft)
    assert(PreReleaseVersionToken.fromString(".").isLeft)
    assert(PreReleaseVersionToken.fromString("").isLeft)
    assert(PreReleaseVersionToken.fromString("1").isRight)
    assert(PreReleaseVersionToken.fromString("-00").isRight)
    assert(PreReleaseVersionToken.fromString("1.0").isLeft)
    assert(PreReleaseVersionToken.fromString("0012aAZ-123").isRight)

    assert(PreReleaseVersionToken.fromBigInt(BigInt(-1)).isLeft)
    assert(PreReleaseVersionToken.fromBigInt(BigInt(0)).isRight)

    assert(PreReleaseVersionToken.NonNumericPreReleaseVersionToken.fromString("0").isLeft)

    assert(Ordering[PreReleaseVersionToken].compare(PreReleaseVersionToken.unsafeFromBigInt(BigInt(1)), PreReleaseVersionToken.unsafeFromString("a")) < 0)
  }
}
