package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import org.scalacheck.Prop._
import org.scalacheck._

final class StrictVersionProperties extends Properties("StrictVersion Properties") {

  property("VersionChangeTypeClass") = forAll((x: StrictVersion, y: StrictVersion) =>
    if (x === y) {
      VersionChangeTypeClass[StrictVersion].changeType(x, y) ?= VersionChangeType.Patch
    } else {
      VersionChangeTypeClass[StrictVersion].changeType(x, y) ?= VersionChangeType.Major
    }
  )
}
