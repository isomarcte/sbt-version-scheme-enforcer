package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class StrictVersionLaws extends DisciplineSuite {
  checkAll("StrictVersion.OrderLaws", OrderTests[StrictVersion].order)
  checkAll("StrictVersion.HashLaws", HashTests[StrictVersion].hash)
}
