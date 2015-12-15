package org.canve.logging.loggers
import org.canve.logging.targets.FileTarget
import io.circe._, io.circe.generic.auto._, io.circe.parse._, io.circe.syntax._
import cats.data.Xor

/*
 * A simple object logger
 */
class ObjectLogger {
  
  def apply[T: Encoder](
    obj: T, 
    name: String, 
    prepend: String = "",
    append: String = "") {
  
      val file = new FileTarget(name)
      file(List(prepend, obj.asJson.toString, append))
    }
}