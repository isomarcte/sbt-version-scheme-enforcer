package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.collection.immutable.SortedSet

package object internal {
  private[core] val separatorRegexString: String = """(?<=[^.])\.(?=[^.])"""

  private[core] def setToSortedSet[A: Ordering](value: Set[A]): SortedSet[A] =
    value match {
      case value: SortedSet[A] => value
      case value =>
        value.foldLeft(SortedSet.empty[A]){
          case (acc, value) => acc ++ SortedSet(value)
        }
    }
}
