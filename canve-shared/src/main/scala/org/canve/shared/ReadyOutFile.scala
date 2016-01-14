package org.canve.shared
import scala.reflect.io.Directory
import java.io.File

object ReadyOutFile {

  /* ready file in existing directory */
  def apply(dir: Directory, fileName: String): File = 
    new File(dir + File.separator + fileName)
  
  /* ready file */
  def apply(dir: String, fileName: String): File = 
    apply(scala.tools.nsc.io.Path(dir).createDirectory(failIfExists = false), fileName)

}

object ReadyOutDir {
  def apply(path: String): Directory = {
    scala.tools.nsc.io.Path(path).createDirectory(failIfExists = false)
  }
}
