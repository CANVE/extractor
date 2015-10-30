package org.canve.sbtPluginTestLib.util

import java.io.File

object ReadyOutFile {
  import java.io.File
  def apply(path: String, fileName: String): File = {
    scala.tools.nsc.io.Path(path).createDirectory(failIfExists = false)
    new File(path + File.separator + fileName)
  }
}

