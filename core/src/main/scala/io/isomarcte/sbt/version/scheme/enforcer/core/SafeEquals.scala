package io.isomarcte.sbt.version.scheme.enforcer.core

/** Type safe equality check. Normally you'd use `cats.kernel.Eq`, but we
  * don't want to add that dependency in the plugin.
  */
private[enforcer] object SafeEquals {
  implicit final class SafeEqualsOps[A](value: A) {
    def ===(other: A): Boolean = value == other //scalafix:ok
    def =!=(other: A): Boolean = (value === other) === false
  }
}
