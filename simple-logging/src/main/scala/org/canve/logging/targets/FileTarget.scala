package org.canve.logging.targets

import scala.tools.nsc.io._
import org.canve.Logging.AbstractTarget
import scala.reflect.io.Path.string2path

/*
 * a file target
 */
class FileTarget(name: String) extends AbstractTarget {
  
  val maybeFile = File(name) // clear the file first, if it already exists 
  if (maybeFile.exists) maybeFile.delete  
  val readyLogFile = maybeFile.createFile(true)
  
  def apply(lines: List[String]) = readyLogFile.appendAll(lines.mkString("\n"))
}
