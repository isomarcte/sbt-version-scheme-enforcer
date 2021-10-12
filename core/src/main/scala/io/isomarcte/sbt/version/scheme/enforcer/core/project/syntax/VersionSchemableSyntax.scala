package io.isomarcte.sbt.version.scheme.enforcer.core.project.syntax

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.project._

trait VersionSchemableSyntax {
  implicit final def versionSchemableSyntax[F[_], A](value: F[A])(implicit F: VersionSchemableClass[F, A]): VersionSchemableClass.VersionSchemableClassOps[F, A] =
    new VersionSchemableClass.VersionSchemableClassOps[F, A](value)

  implicit final def versionSchemableSyntaxId[A](value: Id[A])(implicit F: VersionSchemableClass[Id, A]): VersionSchemableClass.VersionSchemableClassOps[Id, A] =
    new VersionSchemableClass.VersionSchemableClassOps[Id, A](value)
}
