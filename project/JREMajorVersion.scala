package io.isomarcte.sbt.version.scheme.enforcer.build

import scala.util._

/** Determine the JRE Major version in use in this build.
  *
  * Derived from the Java code which does this for the Metals ArchLinux package.
  *
  * JREs >= 9 can use `java.lang.Runtime.version`, but since we sadly still
  * care about 8, we can't use that.
  *
  * @see [[https://aur.archlinux.org/cgit/aur.git/tree/JREMajorVersion.java?h=metals]]
  */
object JREMajorVersion {

  val majorVersion: String =
    sys
      .props
      .get("java.version")
      .map(_.takeWhile(_ != '-')) // handles things like '17-ea'
      .fold(
        throw new RuntimeException(
          "Unable to determine JRE major version since java.version is unset, which should not be possible for standard JVMs."
        )
      ) { value =>
        val mv: String =
          value.split("\\.").toList match {
            case Nil =>
              // In this case there is no '.' in the version number, e.g. it
              // is "15"
              value
            case "1" :: value :: Nil =>
              // Probably using the older version number scheme,
              // e.g. "1.8.0"
              value
            case value :: _ =>
              value
          }

        Try(mv.toInt) match {
          case Success(_) =>
            mv
          case Failure(error) =>
            throw new RuntimeException(
              s"System property java.version (${value}) does not contain a version value which we understand: ${error.getLocalizedMessage}"
            )
        }
      }
}
