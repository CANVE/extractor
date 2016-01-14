package org.canve.shared
import java.io.File

object IO {
  def getSubDirectories(dir: String): List[File] = {
    val directory = new File(dir) 
    if (!directory.exists) throw new Exception(s"API usage error: cannot invoke this function on a non-existant directory ($dir)") // otherwise ugly java exception 
    directory.listFiles.toList.filter(_.isDirectory())
  }
}