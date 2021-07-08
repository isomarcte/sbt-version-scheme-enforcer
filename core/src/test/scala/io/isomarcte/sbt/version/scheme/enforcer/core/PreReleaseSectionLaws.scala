package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class PreReleaseSectionLaws extends DisciplineSuite {
  checkAll("PreReleaseSection.OrderLaws", OrderTests[PreReleaseSection].order)
  checkAll("PreReleaseSection.HashLaws", HashTests[PreReleaseSection].hash)
}
