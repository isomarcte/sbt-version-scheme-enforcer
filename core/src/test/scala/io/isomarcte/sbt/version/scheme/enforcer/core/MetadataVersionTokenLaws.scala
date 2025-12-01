package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class MetadataVersionTokenLaws extends DisciplineSuite {
  checkAll("MetadataVersionToken.OrderLaws", OrderTests[MetadataVersionToken].order)
  checkAll("MetadataVersionToken.HashLaws", HashTests[MetadataVersionToken].hash)
}
