package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.Try
import io.isomarcte.sbt.version.scheme.enforcer.core.SafeEquals._

sealed abstract class VersionComponent extends Product with Serializable with Ordered[VersionComponent] {
  import VersionComponent._

  def value: String

  // Final //

  final def trimmedValue: String =
    value.trim

  override final def compare(that: VersionComponent): Int =
    (this, that) match {
      case (a: NonNegativeIntegral, b: NonNegativeIntegral) =>
        a.value.compare(b.value)
      case (a: PreRelease, b: PreRelease) =>
        a.value.compare(b.value)
      case (a: MetaData, b: MetaData) =>
        a.value.compare(b.value)
      case (a: Unknown, b: Unknown) =>
        a.value.compare(b.value)
      case (_: NonNegativeIntegral, _) =>
        1
      case (_: PreRelease, _: NonNegativeIntegral) =>
        -1
      case (_: PreRelease, _) =>
        1
      case (_: MetaData, _: NonNegativeIntegral | _: PreRelease) =>
        -1
      case (_: MetaData, _) =>
        1
      case (_: Unknown, _) =>
        -1
    }
}

object VersionComponent {

  private[this] val preReleaseMetadataValidPattern: String = """[0-9A-Za-z-]+"""
  private[this] val preReleaseAndMetaDataValidCharPred: Char => Boolean = (c: Char) => (c.isLetterOrDigit || c == '.' || c == '-')
  private[this] val whitespaceWarningString: String = "They _can not_ include whitespace. If you are trying to parse a version string which may have leading or trailing whitespace you should be using VersionComponent.fromVersionString rather than the individual component methods."

  sealed abstract class NonNegativeIntegral extends VersionComponent {
    def asBigInt: BigInt

    // Final //

    override final def toString: String =
      s"NonNegativeIntegral(value = ${value}, asBigInt = ${asBigInt})"
  }

  object NonNegativeIntegral {
    private[this] final case class NonNegativeIntegralImpl(override val value: String, override val asBigInt: BigInt) extends NonNegativeIntegral

    def fromString(value: String): Either[String, NonNegativeIntegral] =
      if (value.isEmpty) {
        Left("Can not creat NonNegativeIntegral (Numeric) VersionComponent from empty value."): Either[String, NonNegativeIntegral]
      } else if (value.startsWith("00")) {
        Left(s"NonNegativeIntegral (Numeric) VersionComponent values may not begin with multiple leading 0s according to SemVer spec: ${value}")
      } else if (value.matches("""\d+""")){
        Try(BigInt(value)).toEither.fold(
          e => Left(s"Failed when attempting to parse ${value} as non-negative integral value: ${e.getLocalizedMessage}"),
          bigInt =>
          if (bigInt < BigInt(0)) {
            Left(s"Negative NonNegativeIntegral (Numeric) values are not valid: ${value} (parsed as ${bigInt})")
          } else {
            Right(NonNegativeIntegralImpl(value, bigInt))
          }
        )
      } else {
        Left(s"NonNegativeIntegral (Numeric) values must contain only digits. ${whitespaceWarningString}")
      }
  }

  sealed abstract class PreRelease extends VersionComponent {
    override final def toString: String =
      s"PreRelease(value = ${value})"
  }

  object PreRelease {
    private[this] final case class PreReleaseImpl(override val value: String) extends PreRelease

    def fromString(value: String): Either[String, PreRelease] = {
      if (value.matches(preReleaseMetadataValidPattern) && value.contains("00") === false) {
        Right(PreReleaseImpl(value))
      } else {
        Left(s"Failed to parse ${value} as PreRelease VersionComponent. PreRelease VersionComponent's must only contain [0-9A-Za-z-] and can not start with multiple leading 0s or be empty. ${whitespaceWarningString}")
      }
    }

    def unsafeFromString(value: String): PreRelease =
      fromString(value).fold(
        e => throw new IllegalArgumentException(e),
        identity
      )
  }

  sealed abstract class MetaData extends VersionComponent {
    override final def toString: String =
      s"MetaData(value = ${value})"
  }

  object MetaData {
    private[this] final case class MetaDataImpl(override val value: String) extends MetaData

    def fromString(value: String): Either[String, MetaData] = {
      if (value.matches(preReleaseMetadataValidPattern) && value.contains("00") === false) {
        Right(MetaDataImpl(value))
      } else {
        Left(s"Failed to parse ${value} as MetaData VersionComponent. MetaData VersionComponent's must only contain [0-9A-Za-z-] and can not start with a leading 0 or be empty. ${whitespaceWarningString}")
      }
    }
  }

  sealed abstract class Unknown extends VersionComponent {
    override final def toString: String =
      s"Unknown(value = ${value})"
  }

  object Unknown {
    private[VersionComponent] final case class UnknownImpl(override val value: String) extends Unknown
  }

  def fromVersionString(
    value: String
  ): Vector[VersionComponent] = {
    def parseSection(value: String, constructor: String => Either[String, VersionComponent]): Either[String, Vector[VersionComponent]] =
      if (value.isEmpty) {
        Right(Vector.empty[VersionComponent])
      } else {
        val components: Vector[String] = value.split('.').toVector

        if (components.exists(_.isEmpty)) {
          // If any component is empty, then we have a section of the input
          // string with two or more neighboring . characters,
          // e.g. `1..0`. This makes treating the input as . separated
          // NonNegativeIntegral components invalid.
          Left("An empty component section exists, but that is not valid.")
        } else {
          Right(
            components.foldLeft(Vector.empty[VersionComponent]){
              case (acc, value) =>
                acc ++ Vector(
                  constructor(value).getOrElse(
                    Unknown.UnknownImpl(value))
                )
            }
          )
        }
      }

    def parseNumeric(value: String): Vector[VersionComponent] =
      parseSection(value, NonNegativeIntegral.fromString).fold(
        Function.const(Vector(Unknown.UnknownImpl(value))),
        identity
      )

    def parsePreRelease(value: String): Vector[VersionComponent] =
      parseSection(value, PreRelease.fromString).fold(
        // Have to re-add the removed "-".
        Function.const(Vector(Unknown.UnknownImpl("-" ++ value))),
        identity
      )

    def parseMetaData(value: String): Vector[VersionComponent] =
      parseSection(value, MetaData.fromString).fold(
        // Have to re-add the removed "+".
        Function.const(Vector(Unknown.UnknownImpl("+" ++ value))),
        identity
      )

    val (numeric, preReleaseAndMetaData) = value.trim.span((c: Char) => c.isDigit || c == '.')
    val (preRelease, metaDataAndRest) =
      if (preReleaseAndMetaData.startsWith("-")) {
        preReleaseAndMetaData.drop(1).span((c: Char) => preReleaseAndMetaDataValidCharPred(c))
      } else {
        // Pre-release data _must_ start with a '-', thus no pre-release data
        // was found.
        ("", preReleaseAndMetaData)
      }
    val (metaData, rest) =
      if (metaDataAndRest.startsWith("+")) {
        metaDataAndRest.drop(1).span((c: Char) => preReleaseAndMetaDataValidCharPred(c))
      } else {
        // MetaData data _must_ start with a '+', thus no metadata was found.
        ("", metaDataAndRest)
      }

    val numericSection: Vector[VersionComponent] =
      parseNumeric(numeric)

    val preReleaseSection: Vector[VersionComponent] =
      parsePreRelease(preRelease)

    val metaDataSection: Vector[VersionComponent] =
      parseMetaData(metaData)

    val restSection: Vector[VersionComponent] =
      if (rest.isEmpty) {
        Vector.empty
      } else {
        Vector(Unknown.UnknownImpl(rest))
      }

    numericSection ++ preReleaseSection ++ metaDataSection ++ restSection
  }

  final def fromVersion(value: Version): Vector[VersionComponent] =
    fromVersionString(value.value)
}
