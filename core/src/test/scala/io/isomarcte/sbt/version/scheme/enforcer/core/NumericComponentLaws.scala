package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class NumericComponentLaws extends DisciplineSuite {
  checkAll("NumericComponent.OrderLaws", OrderTests[NumericComponent].order)
  checkAll("NumericComponent.HashLaws", HashTests[NumericComponent].hash)
}
