package io.isomarcte.sbt.version.scheme.enforcer.core

/** A typeclass for types which represent a version and can communicate an API
  * change. For example, SemVer, EarlySemVer, and PVP are all members of this
  * typeclass because given two instances of any of those version types we can
  * determine what the API change denoted by the versions is.
  */
trait VersionChangeTypeClass[A] extends Serializable { self =>

  /** Given two version types, determine the communicated API change between the two.
    *
    * @note The method is commutative, e.g. `changeType(x, y) == changeType(y,
    *       x)` for all x and y.
    */
  def changeType(x: A, y: A): VersionChangeType

  // final //

  final def contramap[B](f: B => A): VersionChangeTypeClass[B] =
    new VersionChangeTypeClass[B] {
      override def changeType(x: B, y: B): VersionChangeType =
        self.changeType(f(x), f(y))
    }
}

object VersionChangeTypeClass {
  def apply[A](implicit A: VersionChangeTypeClass[A]): VersionChangeTypeClass[A] = A
}
