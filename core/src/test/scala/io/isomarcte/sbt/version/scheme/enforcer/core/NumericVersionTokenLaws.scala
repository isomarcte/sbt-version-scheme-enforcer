package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class NumericVersionTokenLaws extends DisciplineSuite {
  checkAll("NumericVersionToken.OrderLaws", OrderTests[NumericVersionToken].order)
  checkAll("NumericVersionToken.HashLaws", HashTests[NumericVersionToken].hash)
}
