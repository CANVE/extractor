package org.canve.logging.loggers

import org.canve.logging.targets.FileTarget
import org.canve.logging.targets.ConsoleTarget
import org.canve.Logging.AbstractLogger

/*
 * A simple logger class
 */
class StringLogger(logFileName: String) extends AbstractLogger {
  val file    = new FileTarget(logFileName)
  val console = new ConsoleTarget
  
  def apply(lines: List[String]) = {
    file(lines)
    console(lines)
  }
}


