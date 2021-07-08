package io.isomarcte.sbt.version.scheme.enforcer.core

import org.scalacheck.Prop._
import org.scalacheck._

final class NumericVersionTokenProperties extends Properties("NumericVersionToken Properties") {
  property("Valid values are >= 0.") = forAll { (value: BigInt) =>
    NumericVersionToken
      .fromBigInt(value)
      .fold(
        Function.const(Prop(value < 0) :| "Values < 0 are invalid."),
        Function.const(Prop(value >= 0) :| "Values >= 0 are valid.")
      )
  }
}
