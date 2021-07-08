package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class VersionSectionsLaws extends DisciplineSuite {
  checkAll("VersionSections.OrderLaws", OrderTests[VersionSections].order)
  checkAll("VersionSections.HashLaws", HashTests[VersionSections].hash)
}
