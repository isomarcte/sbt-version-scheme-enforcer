package io.isomarcte.sbt.version.scheme.enforcer.core

import coursier.version._
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import munit._

final class NumericVersionTest extends FunSuite {

  test("Correctly removes tags") {
    assert(
      NumericVersion.fromCoursierVersion(Version("1.0.0-SNAPSHOT")) ===
        Right(NumericVersion.unsafeFromVector(Vector(BigInt(1), BigInt(0), BigInt(0))))
    )
  }

  test("Disallows negative versions") {
    assert(NumericVersion.fromVector(Vector(BigInt(-1), BigInt(0), BigInt(0))).isLeft)
  }
}
