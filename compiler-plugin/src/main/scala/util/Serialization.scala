package org.canve.util

trait SerializationUtil {
  def toStringOption(s: String): Option[String] = {
    s match {
      case "None" => None
      case _ => Some(s.drop("Some(".length).dropRight(1))
    }
  }
  
  def toClassArgs(s: String): String = {
    s.dropWhile(_ != '(').drop(1).dropRight(1)
  }
}