package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanVersionComponentInstances._
import munit._

final class VersionComponentLaws extends DisciplineSuite {
  checkAll("VersionComponent.OrderLaws", OrderTests[VersionComponent].order)
  checkAll("VersionComponent.HashLaws", HashTests[VersionComponent].hash)
}
