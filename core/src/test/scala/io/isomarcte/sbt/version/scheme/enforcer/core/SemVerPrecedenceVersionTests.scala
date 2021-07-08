package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class SemVerPrecedenceVersionTests extends FunSuite {
  test("Ordering") {
    val ordering: Ordering[SemVerPrecedenceVersion] = Ordering[SemVerPrecedenceVersion]
    import ordering.mkOrderingOps

    assert(
      SemVerPrecedenceVersion.unsafeFromString("1.0.0-SNAPSHOT") < SemVerPrecedenceVersion.unsafeFromString("1.0.0")
    )
    assert(SemVerPrecedenceVersion.unsafeFromString("1.0.0-RC.1") < SemVerPrecedenceVersion.unsafeFromString("1.0.0"))
    assert(SemVerPrecedenceVersion.unsafeFromString("0.0.0") < SemVerPrecedenceVersion.unsafeFromString("1.0.0"))
    assert(SemVerPrecedenceVersion.unsafeFromString("0.1.0") < SemVerPrecedenceVersion.unsafeFromString("1.0.0"))
    assert(SemVerPrecedenceVersion.unsafeFromString("0.0.1") < SemVerPrecedenceVersion.unsafeFromString("1.0.0"))
  }
}
