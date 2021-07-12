package io.isomarcte.sbt.version.scheme.enforcer.plugin

/** ADT for describing which tags to consider when calculating previous
  * versions.
  */
sealed abstract class TagDomain extends Product with Serializable

object TagDomain {

  /** Consider all tags on the repository, even if they are not ancestors of the
    * current commit.
    */
  case object All extends TagDomain

  /** Only consider tags which are reachable (ancestors) of the current commit.
    */
  case object Reachable extends TagDomain

  /** Only consider tags which are unreachable (not ancestors) of the current commit.
    *
    * I don't know why you'd use this.
    */
  case object Unreachable extends TagDomain

  /** Only consider tags which contain this commit.
    *
    * I don't know why you'd use this.
    */
  case object Contains extends TagDomain

  /** Only consider tags which do not contain this commit. This is similar to
    * All, but will never include any tags which are present on this
    * commit. In typical usage of this plugin it is unlikely this circumstance
    * will occur.
    */
  case object NoContains extends TagDomain
}
