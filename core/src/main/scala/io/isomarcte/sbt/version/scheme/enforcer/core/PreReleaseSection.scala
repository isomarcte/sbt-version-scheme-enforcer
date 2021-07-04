package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

sealed abstract class PreReleaseSection extends Product with Serializable {
  def value: Vector[PreReleaseComponent]

  // final //

  final def canonicalString: String =
    s"""-${value.map(_.value).mkString(".")}"""

  override final def toString: String =
    s"PreReleaseSection(${canonicalString})"
}

object PreReleaseSection {
  private[this] final case class PreReleaseSectionImpl(override val value: Vector[PreReleaseComponent]) extends PreReleaseSection

  val empty: PreReleaseSection = PreReleaseSectionImpl(Vector.empty)

  def apply(value: Vector[PreReleaseComponent]): PreReleaseSection =
    PreReleaseSectionImpl(value)

  def fromString(value: String): Either[String, PreReleaseSection] =
    if (value === "-") {
      // Valid, but empty, pre-release
      Right(empty)
    } else if (value.startsWith("-")) {
      value.drop(1).split('.').foldLeft(Right(Vector.empty[PreReleaseComponent]): Either[String, Vector[PreReleaseComponent]]){
        case (acc, value) =>
          acc.flatMap(acc =>
            PreReleaseComponent.fromString(value).map(value =>
              acc ++ Vector(value)
            )
          )
      }.map(apply)
    } else {
      Left(s"Invalid pre-release section. The pre-release section must begin with a - character: ${value}")
    }

  def unsafeFromString(value: String): PreReleaseSection =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  implicit val ordering: Ordering[PreReleaseSection] =
    new Ordering[PreReleaseSection] {
      override def compare(x: PreReleaseSection, y: PreReleaseSection): Int = {
        val maxSize: Int = scala.math.max(x.value.size, y.value.size)

        x.value.map(Option.apply).padTo(maxSize, None).zip(y.value.map(Option.apply).padTo(maxSize, None)).foldLeft(0){
          case (0, pair) =>
            pair match {
              case (Some(a), Some(b)) =>
                Ordering[PreReleaseComponent].compare(a, b)
              case (Some(_), None) =>
                1
              case (None, Some(_)) =>
                -1
              case (None, None) =>
                // Should not be possible
                0
            }
          case (otherwise, _) =>
            otherwise
        }
      }
    }
}
