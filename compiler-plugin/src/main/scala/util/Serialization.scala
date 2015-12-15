package org.canve.util

trait SerializationUtil {
  def toStringOption(s: String): Option[String] = {
    s.take(4) match {
      case "None" => None
      case "Some" => Some(s.drop("Some(".length).dropRight(1)) 
      case _ => throw new Exception(s"failed deserializing $getClass from $s.")
    }
  }
  
  def toClassArgs(s: String): String = {
    s.dropWhile(_ != '(').drop(1).dropRight(1)
  }
}