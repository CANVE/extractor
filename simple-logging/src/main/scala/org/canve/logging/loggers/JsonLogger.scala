package org.canve.logging.loggers
import org.canve.logging.targets.FileTarget
import play.api.libs.json._
import java.io.File

/*
 * A simple object logger
 */
class JsonLogger(basePath: String) {

  def apply(path: String, jsonObj: JsValue) { 
    val file = new FileTarget(basePath + File.separator + path + ".json")
    file(jsonObj.toString)
  }
}

class ObjectLoggerOld(basePath: String) {
  import io.circe._, io.circe.generic.auto._, io.circe.parse._, io.circe.syntax._
  import cats.data.Xor
  
  def apply[T: Encoder](path: String, obj: T) { 
    val file = new FileTarget(basePath + File.separator + path + ".json")
    file(obj.asJson.toString)
  }
}