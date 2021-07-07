package io.isomarcte.sbt.version.scheme.enforcer.core

import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

/** A data type which models the pre-release section of a version string.
  *
  * The pre-release section is made up of pre-release components represented
  * by instances of [[PreReleaseVersionToken]]. A pre-release section of a
  * version string always begins with a '-' character and then continues as
  * long as each character matches [0-9A-Za-z-.]. Each component is '.'
  * separated and the leading '-' is not part of any component. If it is
  * entirely numeric (matches \d+), then it can not have leading zeros.
  *
  * Pre-release sections of version strings commonly denote release candidate,
  * milestone, or snapshot versions.
  *
  * @note Perhaps counter intuitively, pre-release components starting with a
  *       '-' ''may'' have leading zeros, for example `--001`. This is because
  *       this pre-release ''is not'' numeric, in that it does not match \d+,
  *       it has a leading '-'.
  *
  * For example,
  * {{{
  * scala> val versionString: String = "1.0.0--001+5"
  * versionString: String = 1.0.0--001+5
  *
  * scala> VersionSections.unsafeFromString(versionString)
  * res0: io.isomarcte.sbt.version.scheme.enforcer.core.VersionSections = VersionSections(numericSection = NumericSection(1.0.0), preReleaseSection = Some(PreReleaseSection(--001)), metadataSection = Some(MetadataSection(+5)))
  *
  * scala> VersionSections.unsafeFromString(versionString).preReleaseSection
  * res1: Option[io.isomarcte.sbt.version.scheme.enforcer.core.PreReleaseSection] = Some(PreReleaseSection(--001))
  *
  * scala> PreReleaseSection.unsafeFromString("--001")
  * res2: io.isomarcte.sbt.version.scheme.enforcer.core.PreReleaseSection = PreReleaseSection(--001)
  *
  * scala> PreReleaseSection.unsafeFromString("--001").value
  * res3: Vector[io.isomarcte.sbt.version.scheme.enforcer.core.PreReleaseVersionToken] = Vector(NonNumericPreReleaseVersionToken(value = -001))
  *
  * scala> PreReleaseSection.unsafeFromString("-0.2").value
  * res4: Vector[io.isomarcte.sbt.version.scheme.enforcer.core.PreReleaseVersionToken] = Vector(NumericPreReleaseVersionToken(numericValue = 0), NumericPreReleaseVersionToken(numericValue = 2))
  *
  * scala> PreReleaseSection.unsafeFromString("-SNAPSHOT").value
  * res5: Vector[io.isomarcte.sbt.version.scheme.enforcer.core.PreReleaseVersionToken] = Vector(NonNumericPreReleaseVersionToken(value = SNAPSHOT))
  * }}}
  */
sealed abstract class PreReleaseSection extends Product with Serializable {
  def value: Vector[PreReleaseVersionToken]

  // final //

  /** The representation of the pre-release as derived from the components.
    *
    * If the version string was parsed directly from
    * [[PreReleaseSection#fromString]], this should be the same as the
    * original pre-release section input.
    */
  final def canonicalString: String =
    s"""-${value.map(_.value).mkString(".")}"""

  override final def toString: String =
    s"PreReleaseSection(${canonicalString})"
}

object PreReleaseSection {

  private[this] final case class PreReleaseSectionImpl(override val value: Vector[PreReleaseVersionToken]) extends PreReleaseSection

  /** Create a new [[PreReleaseSection]] from components. */
  def apply(value: Vector[PreReleaseVersionToken]): PreReleaseSection =
    PreReleaseSectionImpl(value)

  /** Attempt to create a [[PreReleaseSection]] from a [[java.lang.String]].
    *
    * The string must being with a '+' and can only contain [0-9A-Za-z-.].
    */
  def fromString(value: String): Either[String, PreReleaseSection] =
    if (value === "-") {
      // Valid, but empty, pre-release
      Right(empty)
    } else if (value.startsWith("-")) {
      value.drop(1).split(internal.separatorRegexString).foldLeft(Right(Vector.empty[PreReleaseVersionToken]): Either[String, Vector[PreReleaseVersionToken]]){
        case (acc, value) =>
          acc.flatMap(acc =>
            PreReleaseVersionToken.fromString(value).map(value =>
              acc ++ Vector(value)
            )
          )
      }.map(apply)
    } else {
      Left(s"Invalid pre-release section. The pre-release section must begin with a - character: ${value}")
    }

  /** As [[#fromString]], but throws an exception if the value is invalid.
    *
    * It is ''strongly'' recommended that this only be used on the REPL and in
    * tests.
    */
  def unsafeFromString(value: String): PreReleaseSection =
    fromString(value).fold(e => throw new IllegalArgumentException(e), identity)

  /** The empty [[PreReleaseSection]].
    *
    * This is equivalent to calling [[#fromString]] with "-".
    */
  val empty: PreReleaseSection = apply(Vector.empty)

  /** A constant for the common [[PreReleaseSection]] "-SNAPSHOT".
    */
  val snapshot: PreReleaseSection = apply(Vector(PreReleaseVersionToken.snapshot))

  implicit val ordering: Ordering[PreReleaseSection] =
    new Ordering[PreReleaseSection] {
      override def compare(x: PreReleaseSection, y: PreReleaseSection): Int = {
        val maxSize: Int = scala.math.max(x.value.size, y.value.size)

        x.value.map(Option.apply).padTo(maxSize, None).zip(y.value.map(Option.apply).padTo(maxSize, None)).foldLeft(0){
          case (0, pair) =>
            pair match {
              case (Some(a), Some(b)) =>
                Ordering[PreReleaseVersionToken].compare(a, b)
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
