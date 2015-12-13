package org.canve.Logging

/*
 * logging target abstract class
 */
abstract class AbstractTarget {
  
  abstract class State
  case object NotOpen extends State  
  case object Open    extends State  
  case object Error   extends State
  
  var state = NotOpen
  
  def apply(lines: List[String])
}