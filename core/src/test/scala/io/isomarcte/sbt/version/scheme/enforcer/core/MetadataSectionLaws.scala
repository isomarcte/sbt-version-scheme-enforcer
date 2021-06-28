package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class MetadataSectionLaws extends DisciplineSuite {
  checkAll("MetadataSection.OrderLaws", OrderTests[MetadataSection].order)
  checkAll("MetadataSection.HashLaws", HashTests[MetadataSection].hash)
}
