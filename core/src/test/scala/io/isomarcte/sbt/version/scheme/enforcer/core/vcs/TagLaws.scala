package io.isomarcte.sbt.version.scheme.enforcer.core.vcs

import cats.kernel.laws.discipline._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs.OrphanTagInstances._
import munit._

// We don't use cats on the main classpath, but by adding orphan instances for
// Order and Hash, based off the underlying compare and hashCode methods, we
// can ensure that our `compare`, `equals`, and `hashCode` are all congruent with
// each other.
final class TagLaws extends DisciplineSuite {
  checkAll("Tag.OrderLaws", OrderTests[Tag].order)
  checkAll("Tag.HashLaws", HashTests[Tag].hash)
}
