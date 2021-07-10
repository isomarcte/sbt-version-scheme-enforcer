package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import org.scalacheck.Prop._
import org.scalacheck._

final class AlwaysVersionProperties extends Properties("AlwaysVersion Properties") {

  property("VersionChangeTypeClass") = forAll((x: AlwaysVersion, y: AlwaysVersion) =>
    VersionChangeTypeClass[AlwaysVersion].changeType(x, y) ?= VersionChangeType.Patch
  )
}
