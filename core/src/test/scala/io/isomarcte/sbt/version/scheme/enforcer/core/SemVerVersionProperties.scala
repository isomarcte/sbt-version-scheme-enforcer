package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.OrphanInstances._
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._
import org.scalacheck.Prop._
import org.scalacheck._

final class SemVerVersionProperties extends Properties("SemVerVersion Properties") {
  property("SemVerVersion.fromString(value.canonicalString) === Right(value)") = forAll { (value: SemVerVersion) =>
    SemVerVersion.fromString(value.canonicalString) ?= Right(value)
  }

  property("SemVer Versions require exactly three numeric components.") = forAll { (value: VersionSections) =>
    val result: Either[String, SemVerVersion] = SemVerVersion.fromSections(value)

    if (value.numericSection.value.size === 3) {
      Prop(result.isRight) && (result.map(_.asVersionSections) ?= Right(value))
    } else {
      Prop(result.isLeft)
    }
  }
}
