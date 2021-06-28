package io.isomarcte.sbt.version.scheme.enforcer.core

import munit._

final class VersionComponentTests extends FunSuite {

  // test("VersionComponent.VersionNumberComponent does not permit leading 0s") {
  //   assert(VersionComponent.VersionNumberComponent.fromString("0").isRight)
  //   assert(VersionComponent.VersionNumberComponent.fromString("00").isLeft)
  //   assert(VersionComponent.VersionNumberComponent.fromString("10").isRight)
  //   assert(VersionComponent.VersionNumberComponent.fromString("010").isLeft)
  // }

  // test("VersionComponent.VersionNumberComponent does not permit PreRelease and Metadata characters") {
  //   assert(VersionComponent.VersionNumberComponent.fromString("-").isLeft)
  //   assert(VersionComponent.VersionNumberComponent.fromString("0-0").isLeft)
  // }

  // test("VersionComponent values can not be empty") {
  //   assert(VersionComponent.VersionNumberComponent.fromString("").isLeft)
  //   assert(VersionComponent.PreRelease.fromString("").isLeft)
  //   assert(VersionComponent.Metadata.fromString("").isLeft)
  // }

  // test("VersionComponent.PreRelease does not permit leading 0s") {
  //   assert(VersionComponent.PreRelease.fromString("0").isRight)
  //   assert(VersionComponent.PreRelease.fromString("00").isLeft)
  //   assert(VersionComponent.PreRelease.fromString("10").isRight)
  //   assert(VersionComponent.PreRelease.fromString("010").isLeft)
  // }

  // test("VersionComponent.Metadata does not permit leading 0s") {
  //   assert(VersionComponent.Metadata.fromString("0").isRight)
  //   assert(VersionComponent.Metadata.fromString("00").isRight)
  //   assert(VersionComponent.Metadata.fromString("10").isRight)
  //   assert(VersionComponent.Metadata.fromString("010").isRight)
  // }

  // test("Valid VersionComponent.PreRelease") {
  //   assert(VersionComponent.PreRelease.fromString("0").isRight)
  //   assert(VersionComponent.PreRelease.fromString("-").isRight)

  //   // Leading zeros are okay if the component is not numeric.
  //   assert(VersionComponent.PreRelease.fromString("0A").isRight)
  //   assert(VersionComponent.PreRelease.fromString("A").isRight)
  //   assert(VersionComponent.PreRelease.fromString("a").isRight)

  //   // Starting with a '-' is defined to be non-numeric, since negative
  //   // numeric components are illegal, but '-'is legal.
  //   assert(VersionComponent.PreRelease.fromString("-0A").isRight)
  // }

  // test("Parsing valid version strings should yield no errors") {
  //   def parseTest(value: String): Boolean = {
  //     VersionComponent.unsafeFromVersionString(value)
  //     true
  //   }

  //   assert(parseTest(""))
  //   assert(parseTest("1.0.0-0.1+001"))
  //   assert(parseTest("1.0.0+001"))
  //   assert(parseTest("1.0.0-1"))
  //   assert(parseTest("1.0.0"))
  //   assert(parseTest("-1.2"))
  //   assert(parseTest("+01.2"))

  //   // Leading zeros are permitted in a non-numeric pre-release component
  //   // value. The entire pre-release component value here is `00-1`, since `-`
  //   // is a valid character _inside_ a per-release component.
  //   assert(parseTest("1-00-1"))

  //   // This looks invalid, because it looks like the metadata is before the
  //   // pre-release value, but it is valid. `-` can occur _within_ a component,
  //   // so this the entire Metadata component is `01-1`.
  //   assert(parseTest("1.0.0+01-1"))
  // }

  // test("Parsing invalid version strings should yield errors") {
  //   // Metadata before PreRelease is invalid
  //   assert(VersionComponent.fromVersionString("1.0a.0").isLeft)
  //   assert(VersionComponent.fromVersionString("1.0.0-$").isLeft)
  //   assert(VersionComponent.fromVersionString("1..0").isLeft)
  // }
}
