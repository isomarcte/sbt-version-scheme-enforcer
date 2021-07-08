package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import org.scalacheck.Prop._
import org.scalacheck._

final class NumericSectionProperties extends Properties("NumericSection Properties") {
  property("NumericSection.fromString(value.canonicalString) === Right(value)") = forAll {
    (numericSection: NumericSection) =>
      NumericSection.fromString(numericSection.canonicalString) ?= Right(numericSection)
  }
}
