package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import org.scalacheck.Prop._
import org.scalacheck._

final class VersionSectionsProperties extends Properties("VersionSections Properties") {
  property("VersionSections.fromString(value.canonicalString) === Right(value)") = forAll { (value: VersionSections) =>
    VersionSections.fromString(value.canonicalString) ?= Right(value)
  }
}
