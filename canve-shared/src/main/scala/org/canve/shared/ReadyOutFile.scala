package org.canve.shared

import java.io.File
import scala.reflect.io.Directory

object ReadyOutFile {
  import java.io.File
  def apply(path: String, fileName: String): File = {
    scala.tools.nsc.io.Path(path).createDirectory(failIfExists = false)
    new File(path + File.separator + fileName)
  }
}

object ReadyOutDir {
  import java.io.File
  def apply(path: String): Directory = {
    scala.tools.nsc.io.Path(path).createDirectory(failIfExists = false)
  }
}
