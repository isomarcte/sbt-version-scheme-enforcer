lazy val pluginVersion: String = "plugin.version"

sys.props.get(pluginVersion).fold(
  sys.error(s"${pluginVersion} is not set")
)(pluginVersion =>
  addSbtPlugin("io.isomarcte" % "sbt-version-scheme-enforcer-plugin" % pluginVersion)
)
