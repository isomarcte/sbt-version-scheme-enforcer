package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class PreReleaseVersionTokenLaws extends DisciplineSuite {
  checkAll("PreReleaseVersionToken.OrderLaws", OrderTests[PreReleaseVersionToken].order)
  checkAll("PreReleaseVersionToken.HashLaws", HashTests[PreReleaseVersionToken].hash)
}
