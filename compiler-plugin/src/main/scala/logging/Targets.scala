package Logging

import scala.tools.nsc.io._

/*
 * logging target abstract class
 */
abstract class Target {
  
  abstract class State
  case object NotOpen extends State  
  case object Open    extends State  
  case object Error   extends State
  
  var state = NotOpen
  
  def apply(lines: List[String])
}

/*
 * a file target
 */
class FileTarget(name: String) extends Target {
  
  val maybeFile = File(name) // clear the file first, if it already exists 
  if (maybeFile.exists) maybeFile.delete  
  val readyLogFile = maybeFile.createFile(true)
  
  def apply(lines: List[String]) = readyLogFile.appendAll(lines.mkString)
}

/*
 * the console target
 */
class ConsoleTarget() extends Target {
  def apply(lines: List[String]) = lines.map(l => println(Console.BLUE + Console.BOLD + "[canve] " + Console.RESET + l))
}
