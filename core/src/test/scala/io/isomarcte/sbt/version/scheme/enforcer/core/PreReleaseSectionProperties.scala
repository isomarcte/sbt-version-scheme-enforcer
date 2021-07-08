package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import org.scalacheck.Prop._
import org.scalacheck._

final class PreReleaseSectionProperties extends Properties("PreReleaseSection Properties") {
  property("PreReleaseSection.fromString(value.canonicalString) === Right(value)") = forAll {
    (value: PreReleaseSection) =>
      PreReleaseSection.fromString(value.canonicalString) ?= Right(value)
  }
}
