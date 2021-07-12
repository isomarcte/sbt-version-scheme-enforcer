package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version.{Version => CVersion}
import munit._
import scala.annotation.nowarn

@nowarn("cat=deprecation")
final class NumericVersionTest extends FunSuite {

  test("Correctly removes tags") {
    assertEquals(
      NumericVersion.fromCoursierVersion(CVersion("1.0.0-SNAPSHOT")),
      Right(NumericVersion.unsafeFromVectorIsTag(Vector(BigInt(1), BigInt(0), BigInt(0)), true))
    )
  }

  test("Disallows negative versions") {
    assert(NumericVersion.fromVector(Vector(BigInt(-1), BigInt(0), BigInt(0))).isLeft)
  }

  test("Drop all values after the first tag") {
    assertEquals(
      NumericVersion.fromString("0.0.0.1-RC2"),
      Right(NumericVersion.unsafeFromVectorIsTag(Vector(0, 0, 0, 1).map(value => BigInt(value)), true))
    )
  }
}
