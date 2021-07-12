package io.isomarcte.sbt.version.scheme.enforcer.core.vcs

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import cats.implicits._
import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs.OrphanTagInstances._
import munit._

// We don't use cats on the main classpath, but by adding orphan instances for
// Order and Hash, based off the underlying compare and hashCode methods, we
// can ensure that our `compare`, `equals`, and `hashCode` are all congruent with
// each other.
final class TagLaws extends DisciplineSuite {
  checkAll("Tag.OrderLaws", OrderTests[Tag[String]].order)
  checkAll("Tag.HashLaws", HashTests[Tag[String]].hash)

  checkAll("Tag.OrderLaws", OrderTests[Tag[Version]].order)
  checkAll("Tag.HashLaws", HashTests[Tag[Version]].hash)

  checkAll("Tag.OrderLaws", OrderTests[Tag[PVPVersion]].order)
  checkAll("Tag.HashLaws", HashTests[Tag[PVPVersion]].hash)

  checkAll("Tag.OrderLaws", OrderTests[Tag[SemVerVersion]].order)
  checkAll("Tag.HashLaws", HashTests[Tag[SemVerVersion]].hash)

  checkAll("Tag.OrderLaws", OrderTests[Tag[EarlySemVerVersion]].order)
  checkAll("Tag.HashLaws", HashTests[Tag[EarlySemVerVersion]].hash)
}
