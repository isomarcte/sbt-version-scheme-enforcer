package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class SemVerVersionLaws extends DisciplineSuite {
  checkAll("SemVerVersion.OrderLaws", OrderTests[SemVerVersion].order)
  checkAll("SemVerVersion.HashLaws", HashTests[SemVerVersion].hash)
}
