package org.canve.shared

/*
 * Note: being a compiler plugin, only core scala or java libraries accessible to core scala can be
 * used; external libraries will not work even though the compiler plugin itself will compile  
 */
import java.io.{File}
import java.nio.file.{Paths, Files}
import java.nio.file.FileAlreadyExistsException
import scala.reflect.io.Path.string2path
import java.nio.file.Path

class DataIO(canveRoot: String = "canve-data") {  
  
  createDirIfNotExists(canveRoot)
  
  /*
   * write string to file, overwriting if file already exists
   */
  def writeOutputFile(dir: String, fileName: String, fileText: String) {
    createDirIfNotExists(canveRoot + File.separator + dir)
    scala.tools.nsc.io.File(canveRoot + File.separator + dir + File.separator + fileName).writeAll(fileText)
  }

  /*
   * create target folder, if it doesn't already exist
   */
  def createDirIfNotExists(outDir: String) : Path = {
    val outDirObj = Paths.get(outDir)
    try {
      Files.createDirectory(outDirObj)
    } catch { // ignore if directory already exists
      case e: FileAlreadyExistsException => outDirObj
      case e: Throwable => throw(e)  
    }
  }
  
  /*
   * clear the entire canve data directory, while leaving its root there
   */
  def clearAll = {
    println(s"purging data directory $canveRoot") 
    clearRecursive(new File(canveRoot))
  }
  
  private def clearRecursive(obj: File): Unit = {
    if (obj.isDirectory) obj.listFiles.foreach(clearRecursive)
    if (obj.toString != canveRoot.toString) // avoids both deleting it and relying on its existence 
      safeDelete(obj)
  }
  
  private def safeDelete(obj: File) = {
    if (obj.toString.startsWith(canveRoot)) // TODO: improve a la http://stackoverflow.com/questions/33083397/filtering-upwards-path-traversal-in-java-or-scala/33084369#33084369 
      obj.delete
    else 
      throw new Exception(s"safe delete captured an invalid delete attempt (${obj.toString})")
  }

} 
