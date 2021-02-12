package io.isomarcte.sbt.version.scheme.enforcer.plugin

sealed trait MimaDirection extends Product with Serializable {
  def asString: String

  final override lazy val toString: String = s"MimaDirection($asString)"
}

object MimaDirection {
  case object Backward extends MimaDirection {
    final override val asString: String = "backward"
  }

  case object Forwards extends MimaDirection {
    final override val asString: String = "forward"
  }

  case object Both extends MimaDirection {
    final override val asString: String = "both"
  }
}
