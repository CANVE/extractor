package org.canve.logging.targets

import scala.tools.nsc.io._
import org.canve.Logging.AbstractTarget
import scala.reflect.io.Path.string2path

/*
 * a file target
 */
class FileTarget(name: String) extends AbstractTarget {
  
  val maybeFile = File(name)  
  //if (maybeFile.exists) maybeFile.delete // clear the file first, if it already exists  
  val readyLogFile = maybeFile.createFile(false)
  
  def apply(lines: List[String]) = readyLogFile.appendAll(lines.mkString("\n"))
}
