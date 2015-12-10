package org.canve.Logging

abstract class AbstractLogger() {
  
  def apply(lines: List[String])
  
  def apply(s: String) {
    apply(List(s))
  } 
}

class NullLogger extends AbstractLogger {
  def apply(lines: List[String]) = {}
}
