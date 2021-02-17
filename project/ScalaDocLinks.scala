package io.isomarcte.sbt.version.scheme.enforcer.build

import _root_.io.isomarcte.sbt.version.scheme.enforcer.build.GAVs._
import java.io.File
import java.net.URL
import sbt.Keys._
import sbt._
import sbt.librarymanagement._

object ScalaDocLinks {

  /** Construct mappings for dependencies for which SBT/Scaladoc can not do so automatically.
    *
    * @param classpaths In practice this will need to be the ScalaUnidoc
    *        classpath, which is the aggregation of all classpaths.
    * @param scalaBinaryVersion The current scala binary version,
    *        e.g. `scalaBinaryVersion.value`.
    */
  def mappings(classpaths: Seq[Classpath], scalaBinaryVersion: String): Map[File, URL] = {
    classpaths
      .flatten
      .foldLeft(Map.empty[File, URL]) { case (acc, value) =>
        val file: File = value.data
        coursierVersionsMapping(scalaBinaryVersion)(file).toMap ++ acc
      }
  }

  private val jreModules: Set[String] = Set("java.base")

  def jreModuleLinks(jreVersion: String): Map[File, URL] =
    jreModules.foldLeft(Map.empty[File, URL]) { case (acc, value) =>
      acc ++
        Map(
          new File(s"/module/${value}") ->
            new URL(s"https://docs.oracle.com/en/java/javase/${jreVersion}/docs/api/${value}")
        )
    }

  private def maybeScalaBinaryVersionToSuffix(value: Option[String]): String = value.fold("")(value => s"_${value}")

  private def javadocIOAPIUrl(scalaBinaryVersion: Option[String], moduleId: ModuleID): URL = {
    val suffix: String = maybeScalaBinaryVersionToSuffix(scalaBinaryVersion)
    new URL(s"https://javadoc.io/doc/${moduleId.organization}/${moduleId.name}${suffix}/${moduleId.revision}/")
  }

  private def coursierVersionsMapping(scalaBinaryVersion: String)(file: File): Option[(File, URL)] =
    if (file.toString.matches(""".+io.get-coursier.+/versions[^/]+\.jar$""")) {
      Some(
        file ->
          javadocIOAPIUrl(Some(scalaBinaryVersion), GAVs.coursierG %% GAVs.coursierVersionsA % GAVs.coursierVersionsV)
      )
    } else {
      None
    }
}
