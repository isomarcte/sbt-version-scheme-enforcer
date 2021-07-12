package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class SemVerPrecedenceVersionLaws extends DisciplineSuite {
  checkAll("SemVerPrecedenceVersion.OrderLaws", OrderTests[SemVerPrecedenceVersion].order)
  checkAll("SemVerPrecedenceVersion.HashLaws", HashTests[SemVerPrecedenceVersion].hash)
}
