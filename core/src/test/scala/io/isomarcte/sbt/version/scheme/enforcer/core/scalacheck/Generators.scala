package io.isomarcte.sbt.version.scheme.enforcer.core.scalacheck

import org.scalacheck._

private[core] object Generators {
  val nonNegativeBigInt: Gen[BigInt] = Arbitrary.arbitrary[BigInt].map(_.abs)
}
