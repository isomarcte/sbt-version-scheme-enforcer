package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class AlwaysVersionLaws extends DisciplineSuite {
  checkAll("AlwaysVersion.OrderLaws", OrderTests[AlwaysVersion].order)
  checkAll("AlwaysVersion.HashLaws", HashTests[AlwaysVersion].hash)
}
