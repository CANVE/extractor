package org.canve.logging.loggers
import org.canve.logging.targets.FileTarget
import pprint.Config.Defaults._
//import upickle.default._

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
      println("in pprint")
      pprint.pprintln(obj)
      file(List(textBefore, pprint.tokenize(obj, indent=4).mkString, textAfter))
    }
}