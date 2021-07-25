package io.isomarcte.sbt.version.scheme.enforcer.core.project

// import io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeType
// import io.isomarcte.sbt.version.scheme.enforcer.core.VersionChangeTypeClass
// import io.isomarcte.sbt.version.scheme.enforcer.core.internal.setToSortedSet
import scala.collection.immutable.SortedSet
// import io.isomarcte.sbt.version.scheme.enforcer.core.VersionScheme

sealed abstract class ChangeSets[A] extends Product with Serializable {
  protected implicit def orderingInstance: Ordering[A]

  def majorChanges: SortedSet[A]
  def minorChanges: SortedSet[A]
  def patchChanges: SortedSet[A]

  // final //

  override final def toString: String =
    s"ChangeSets(majorChanges = ${majorChanges}, minorChanges = ${minorChanges}, patchChanges = ${patchChanges})"
}

object ChangeSets {
  // private[this] final case class ChangeSetsImpl[A](override val majorChanges: SortedSet[A], override val minorChanges: SortedSet[A], patchChanges: SortedSet[A], override protected implicit val orderingInstance: Ordering[A]) extends ChangeSets[A]

  // def apply[A](major: Set[A], minor: Set[A], patch: Set[A])(implicit A: Ordering[A]): ChangeSets[A] =
  //   ChangeSetsImpl(
  //     setToSortedSet(major),
  //     setToSortedSet(minor),
  //     setToSortedSet(patch),
  //     A
  //   )

  // def fromVersionScheme(targetVersion: Version, otherVersions: Set[Version], versionScheme: VersionScheme): Either[String, (SortedSet[Version], ChangeSet[Version])] =
  //   VersionScheme.validateByScheme(versionScheme)(targetVersion).flatMap(targetVersion =>
  //     otherVersions.foldLeft((SortedSet.empty[Version], SortedSet.empty[Version])){
  //       case ((invalid, valid), value) =>
  //         versionScheme match {
  //           case VersionScheme.PVP =>

  //         }
  //     }
  //   )

  // def fromVersions[A](targetVersion: A, otherVersions: Set[A])(implicit A: Ordering[A], V: VersionChangeTypeClass[A]): ChangeSets[A] =
  //   otherVersions.foldLeft((SortedSet.empty[A], SortedSet.empty[A], SortedSet.empty[A])){
  //     case ((major, minor, patch), value) =>
  //       V.changeType(targetVersion, value) match {
  //         case VersionChangeType.Major =>
  //           (major ++ SortedSet(value), minor, patch)
  //         case VersionChangeType.Minor =>
  //           (major, minor ++ SortedSet(value), patch)
  //         case VersionChangeType.Patch =>
  //           (major, minor, patch ++ SortedSet(value))
  //       }
  //   } match {
  //     case (major, minor, patch) =>
  //       ChangeSets(major, minor, patch)
  //   }
}
