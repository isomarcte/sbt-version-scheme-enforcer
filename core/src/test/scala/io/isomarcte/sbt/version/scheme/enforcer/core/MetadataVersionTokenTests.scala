package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class MetadataVersionTokenTests extends FunSuite {
  test("Construction validation") {
    assert(MetadataVersionToken.fromString("00").isRight)
    assert(MetadataVersionToken.fromString(".").isLeft)
    assert(MetadataVersionToken.fromString("").isLeft)
    assert(MetadataVersionToken.fromString("1").isRight)
    assert(MetadataVersionToken.fromString("-00").isRight)
    assert(MetadataVersionToken.fromString("1.0").isLeft)
    assert(MetadataVersionToken.fromString("0012aAZ-123").isRight)
  }
}
