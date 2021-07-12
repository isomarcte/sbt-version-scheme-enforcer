package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import org.scalacheck.Prop._
import org.scalacheck._

final class MetadataSectionProperties extends Properties("MetadataSection Properties") {
  property("MetadataSection.fromString(value.canonicalString) === Right(value)") = forAll { (value: MetadataSection) =>
    MetadataSection.fromString(value.canonicalString) ?= Right(value)
  }
}
