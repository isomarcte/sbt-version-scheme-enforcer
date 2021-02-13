package io.isomarcte.sbt.version.scheme.enforcer.core

private[core] object SafeEquals {
  implicit final class SafeEqualsOps[A](value: A) {
    def ===(other: A): Boolean = value.equals(other)
  }
}
