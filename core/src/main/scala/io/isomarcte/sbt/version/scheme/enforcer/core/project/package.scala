package io.isomarcte.sbt.version.scheme.enforcer.core

package object project {
  type SBTBinaryChecksV[A] = BinaryChecks[BinaryCheckVersion[A]]
  type SBTBinaryCheckInfoFilterE[A] = ProjectVersionInfo[A] => Either[String, BinaryChecks[BinaryCheckVersion[A]]]
  type SBTSchemedBinaryCheckFilterE[A] = VersionScheme => SBTBinaryCheckInfoFilterE[A]
}
