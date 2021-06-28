package io.isomarcte.sbt.version.scheme.enforcer.core

import scala.util.Try
import scala.collection.immutable.SortedSet

sealed abstract class VersionComponent extends Product with Serializable {
  def value: String

  // Final //

  final def trimmedValue: String =
    value.trim
}

object VersionComponent {

  private[this] val preReleaseMetadataValidPattern: String = """[1-9A-Za-z-][0-9A-Za-z-]+"""

  sealed abstract class PositiveIntegral extends VersionComponent {
    def asBigInt: BigInt

    // Final //

    override final def toString: String =
      s"PositiveIntegral(value = ${value}, asBigInt = ${asBigInt})"
  }

  object PositiveIntegral {
    private[this] final case class PositiveIntegralImpl(override val value: String, override val asBigInt: BigInt) extends PositiveIntegral

    def fromString(value: String): Either[Throwable, PositiveIntegral] =
      Try(BigInt(value.trim)).toEither.map(bigInt =>
        PositiveIntegralImpl(value, bigInt)
      )
  }

  sealed abstract class PreRelease extends VersionComponent {
    override final def toString: String =
      s"PreRelease(value = ${value})"
  }

  object PreRelease {
    private[this] final case class PreReleaseImpl(override val value: String) extends PreRelease

    def fromString(value: String): Either[Throwable, PreRelease] =
      if (value.matches(preReleaseMetadataValidPattern)) {
        Right(PreReleaseImpl(value))
      } else {
        Left(new IllegalArgumentException(s"Invalid PreRelease VersionComponent. A PreRelease VersionComponent must match ${preReleaseMetadataValidPattern}"))
      }
  }

  sealed abstract class MetaData extends VersionComponent {
    override final def toString: String =
      s"MetaData(value = ${value})"
  }

  object MetaData {
    private[this] final case class MetaDataImpl(override val value: String) extends MetaData

    def fromString(value: String): Either[Throwable, MetaData] =
      if (value.matches(preReleaseMetadataValidPattern)) {
        Right(MetaDataImpl(value))
      } else {
        Left(new IllegalArgumentException(s"Invalid MetaData VersionComponent. A MetaData VersionComponent must match ${preReleaseMetadataValidPattern}"))
      }
  }

  sealed abstract class Unknown extends VersionComponent {
    override final def toString: String =
      s"Unknown(value = ${value})"
  }

  object Unknown {
    private[VersionComponent] final case class UnknownImpl(override val value: String) extends Unknown
  }

  private[this] sealed abstract class VersionComponentParseState extends Product with Serializable

  private[this] object VersionComponentParseState {
    case object ParsingVersionNumber extends VersionComponentParseState
    case object ParsingPreRelease extends VersionComponentParseState
    case object ParsingMetaData extends VersionComponentParseState

    def validPreReleaseOrMetaDataChar(value: Char): Boolean =
      value.isLetterOrDigit || value == '-'
  }

  def fromVersionString(
    value: String
  ): Vector[VersionComponent] = {

    def parseNumeric(value: String): Vector[VersionComponent] =
      value.split('.').toVector.map(value =>
        PositiveIntegral.fromString(value).fold(
          Function.const(Unknown.UnknownImpl(value)),
          identity
        )
      )

    def parsePreRelease(value: String): Vector[VersionComponent] =
      value.split('.').toVector.map(value =>
        PreRelease.fromString(value).fold(
          Function.const(Unknown.UnknownImpl(value)),
          identity
        )
      )

    value.trim.span(_ != '-') match {
      case (numeric, rest) =>
        rest.span(_ != '+') match {
          case (prerelease, metadata) =>

        }
    }

    // def parseSection(spanPred: Char => Boolean, componentConstructor: String => Either[Throwable, VersionComponent], rest: Vector[Char]): Option[(VersionComponent, Vector[Char])] = {
    //   val (result, nextRest): (Vector[Char], Vector[Char]) = rest.span(spanPred)
    //   if (result.isEmpty) {
    //     Option.empty[(VersionComponent, Vector[Char])]
    //   } else {
    //     componentConstructor(result.mkString) match {
    //       case Left(_) =>
    //         Some((Unknown.UnknownImpl(rest.mkString), Vector.empty[Char]))
    //       case Right(value) =>
    //         Some((value, nextRest))
    //     }
    //   }
    // }

    // @scala.annotation.tailrec
    // def loop(state: VersionComponentParseState, acc: Vector[VersionComponent], rest: Vector[Char]): Vector[VersionComponent] =
    //   state match {
    //     case VersionComponentParseState.ParsingVersionNumber =>
    //       parseSection(
    //         _.isDigit,
    //         PositiveIntegral.fromString,
    //         rest
    //       ).fold(
    //         loop(VersionComponentParseState.ParsingPreRelease, acc, rest)
    //       ){
    //         case (component, nextRest) =>
    //           val nextAcc: Vector[VersionComponent] =
    //             acc ++ Vector(component)
    //           nextRest.headOption match {
    //             case Some('.') =>
    //               loop(state, nextAcc, nextRest.tail)
    //             case Some('-') =>
    //               loop(VersionComponentParseState.ParsingPreRelease, nextAcc, nextRest.tail)
    //             case Some('+') =>
    //               loop(VersionComponentParseState.ParsingMetaData, nextAcc, nextRest.tail)
    //             case Some(_) =>
    //               nextAcc ++ Unknown.UnknownImpl(nextRest.mkString)
    //             case None =>
    //               nextAcc
    //           }
    //       }
    //     case VersionComponentParseState.ParsingPreRelease =>
    //       parseSection(
    //         VersionComponentParseState.validPreReleaseOrMetaDataChar,
    //         PreRelease.fromString,
    //         rest
    //       ).fold(
    //         loop(VersionComponentParseState.ParsingMetaData, acc, rest)
    //       )
    //   }
  }
}
