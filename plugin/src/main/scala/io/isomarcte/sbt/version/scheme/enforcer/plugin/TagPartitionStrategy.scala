package io.isomarcte.sbt.version.scheme.enforcer.plugin

import io.isomarcte.sbt.version.scheme.enforcer.core._
import io.isomarcte.sbt.version.scheme.enforcer.core.vcs._

sealed abstract class TagPartitionStrategy extends Product with Serializable

object TagPartitionStrategy {
  final case class PartitionByChangeType(changeTypes: NonEmptySet[VersionChangeType]) extends TagPartitionStrategy

  object PartitionByChangeType {

    def apply(changeType: VersionChangeType): PartitionByChangeType =
      PartitionByChangeType(NonEmptySet.one(changeType))

    val default: PartitionByChangeType =
      PartitionByChangeType(NonEmptySet.of(VersionChangeType.Minor, VersionChangeType.Patch))

    private[plugin] def partitionTags(tags: SortedSet[Tag], currentVersion: NumericVersion, strategy: PartitionByChangeType): NonEmptySet[NonEmptySet[Tag]] =
      tags.map(tag =>

      )
  }

  final case class PartitionByFunction(
    f: SortedSet[Tag] => String => NonEmptySet[NonEmptySet[Tag]]
  ) extends TagPartitionStrategy

  val default: TagPartitionStrategy =
    PartitionByChangeType.default

  private[plugin] def partitionTags(tags: SortedSet[Tag], currentVersion: NumericVersion, stratgey: TagPartitionStrategy): NonEmptySet[NonEmptySet[Tag]] =

}
