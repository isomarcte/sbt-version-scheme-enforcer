package io.isomarcte.sbt.version.scheme.enforcer.core

import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import munit._

final class MetadataComponentLaws extends DisciplineSuite {
  checkAll("MetadataComponent.OrderLaws", OrderTests[MetadataComponent].order)
  checkAll("MetadataComponent.HashLaws", HashTests[MetadataComponent].hash)
}
