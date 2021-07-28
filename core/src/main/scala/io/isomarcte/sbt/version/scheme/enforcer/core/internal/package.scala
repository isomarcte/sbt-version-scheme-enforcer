package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.collection.immutable.SortedSet
import scala.collection.GenTraversableOnce

package object internal {
  private[core] val separatorRegexString: String = """(?<=[^.])\.(?=[^.])"""

  private[core] def toSortedSet[A: Ordering](value: GenTraversableOnce[A]): SortedSet[A] =
    value match {
      case value: SortedSet[A] => value
      case value =>
        value.foldLeft(SortedSet.empty[A]){
          case (acc, value) => acc ++ SortedSet(value)
        }
    }

  private[core] def emapSortedSet[A, B: Ordering](f: A => Either[String, B])(value: GenTraversableOnce[A]): Either[String, SortedSet[B]] =
    value.foldLeft(Right(SortedSet.empty): Either[String, SortedSet[B]]){
      case (acc, value) =>
        acc.flatMap(acc =>
          f(value).map(value =>
            acc ++ SortedSet(value)
          )
        )
    }
}
