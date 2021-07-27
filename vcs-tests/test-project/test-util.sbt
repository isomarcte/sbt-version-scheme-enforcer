import java.io._
import java.nio.charset.StandardCharsets
import scala.util._

ThisBuild / scalaVersion := "2.13.5"

ThisBuild / versionScheme := {
  sys.env.get("VERSION_SCHEME")
}

val outputVersionSchemeEnforcerPreviousVersionTask: TaskKey[Unit] = taskKey[Unit](
  "Output the current value of versionSchemeEnforcerPreviousVersion to a file. This is for testing purposes."
)

ThisBuild / outputVersionSchemeEnforcerPreviousVersionTask := {
  val fos: FileOutputStream = new FileOutputStream(sys.env("VERSION_SCHEME_OUT_FILE"))
  Try(
    fos.write(versionSchemeEnforcerPreviousVersion.value.toString.getBytes(StandardCharsets.UTF_8))
  ) match {
    case _ => fos.close
  }
}

val outputVersionSchemeEnforcerVCSTagsTask: TaskKey[Unit] = taskKey[Unit](
  "Output the current value of versionSchemeEnforcerVCSTags to a file. This is for testing."
)

ThisBuild / outputVersionSchemeEnforcerVCSTagsTask := {
  val fos: FileOutputStream = new FileOutputStream(sys.env("VERSION_SCHEME_VCS_TAGS_FILE"))
  Try(
    fos.write(versionSchemeEnforcerVCSTags.value.toString.getBytes(StandardCharsets.UTF_8))
  ) match {
    case _ => fos.close
  }
}
