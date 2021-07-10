package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class PVPVersionLaws extends DisciplineSuite {
  checkAll("PVPVersion.OrderLaws", OrderTests[PVPVersion].order)
  checkAll("PVPVersion.HashLaws", HashTests[PVPVersion].hash)
}
