package io.isomarcte.sbt.version.scheme.enforcer.core

package object project {
  type SBTBinaryChecksV[A] = BinaryChecks[BinaryCheckVersion[A]]
  type SBTBinaryCheckInfoV[A] = BinaryCheckInfo[BinaryCheckVersion[A], BinaryCheckVersion[Version]]
  type SBTBinaryCheckInfoFilter[A] = ProjectVersionInfo[A] => SBTBinaryCheckInfoV[A] => SBTBinaryCheckInfoV[A]
  type SBTBinaryCheckInfoFilterE[A] = ProjectVersionInfo[A] => SBTBinaryCheckInfoV[A] => Either[String, SBTBinaryCheckInfoV[A]]
  type SBTSchemedBinaryCheckFilter[A] = VersionScheme => SBTBinaryCheckInfoFilter[A]
  type SBTSchemedBinaryCheckFilterE[A] = VersionScheme => SBTBinaryCheckInfoFilterE[A]
}
