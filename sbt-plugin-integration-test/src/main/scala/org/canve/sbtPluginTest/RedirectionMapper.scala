package org.canve.sbtPluginTest

import org.canve.shared.ReadyOutFile
/*
 * Mapping between project names and the file path
 * where their output will be redirected into
 */
object RedirectionMapper {
  def apply(project: Project) = {
    ReadyOutFile("out", project.name + ".out")
  }
}


