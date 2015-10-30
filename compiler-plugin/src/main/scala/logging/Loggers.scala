package Logging

import scala.tools.nsc.io._

abstract class Logger() {
  def apply(lines: List[String])
  def apply(s: String) 
}

/*
 * A simple logger class
 */
class DefaultLogger(logFileName: String) extends Logger {
  val file    = new FileTarget(logFileName)
  val console = new ConsoleTarget
  
  def apply(lines: List[String]) = {
    file(lines)
    console(lines)
  }
  
  def apply(s: String) = {
    val text = s + "\n"
    file(List(text))
    console(List(s))
  } 
}

class NullLogger {
  def apply(lines: List[String]) = {}
  def apply(lines: String) = {}
}
