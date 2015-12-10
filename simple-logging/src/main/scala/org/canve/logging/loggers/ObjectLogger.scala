package org.canve.logging.loggers
import org.canve.logging.targets.FileTarget
import pprint._, pprint.Config.Colors._

/*
 * A simple object logger
 */
class ObjectLogger {
  def apply(
    obj: Any, 
    name: String, 
    textBefore: String = "",
    textAfter: String = "") {
  
      val file = new FileTarget(name)
      file(List(textBefore, obj.toString, textAfter))
  }
}