sys.env.get("PLUGIN_VERSION").fold(
  sys.error(s"PLUGIN_VERSION is not set")
)(pluginVersion =>
  addSbtPlugin("io.isomarcte" % "sbt-version-scheme-enforcer-plugin" % pluginVersion)
)
