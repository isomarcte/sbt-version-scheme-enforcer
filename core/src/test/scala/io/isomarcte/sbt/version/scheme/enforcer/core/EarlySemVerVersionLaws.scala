package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class EarlySemVerVersionLaws extends DisciplineSuite {
  checkAll("EarlySemVerVersion.OrderLaws", OrderTests[EarlySemVerVersion].order)
  checkAll("EarlySemVerVersion.HashLaws", HashTests[EarlySemVerVersion].hash)
}
