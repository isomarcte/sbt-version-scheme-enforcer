package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import org.scalacheck.Prop._
import org.scalacheck._

final class PVPVersionProperties extends Properties("PVPVersion Properties") {
  property("PVPVersion.fromString(value.canonicalString) === Right(value)") = forAll { (value: PVPVersion) =>
    PVPVersion.fromString(value.canonicalString) ?= Right(value)
  }
}
