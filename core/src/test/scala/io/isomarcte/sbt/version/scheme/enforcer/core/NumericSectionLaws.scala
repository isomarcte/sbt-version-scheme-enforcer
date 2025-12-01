package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class NumericSectionLaws extends DisciplineSuite {
  checkAll("NumericSection.OrderLaws", OrderTests[NumericSection].order)
  checkAll("NumericSection.HashLaws", HashTests[NumericSection].hash)
}
