package org.canve.compilerPlugin
import tools.nsc.Global

/*
 * Classes and deserializers which distinguish regular names -
 * from names synthetically provided by us for (unnamed) anonymous symbols. 
 */

/* 
 * regular name provided by the compiler for a symbol 
 */
case class RealName(val name: String) extends SymbolName

/* 
 * synthetically contrived name - created for an anonymous symbol 
 */
case class DeAnonimizedName(val name: String) extends SymbolName
object DeAnonimizedName {
  
  /* construction from a symbol */
  def apply(global: Global)(s: global.Symbol) = new DeAnonimizedName(s.pos.hashCode.toHexString)
}

/* 
 * a deserializer restoring the appropriate class 
 */
object SymbolName {
  
  def apply(s: String): SymbolName = {
    val className = s.takeWhile(_ != '(')
    val value = s.drop(className.length).drop(1).dropRight(1)
    className match {
      case "RealName" => new RealName(value)
      case "DeAnonimizedName" => DeAnonimizedName(value)
      case _ => throw new Exception(s"failed deserializing SymbolName (parsed to $className $value)")
    }
  }
}

/* the base inherited class */
sealed abstract class SymbolName {
  val name: String
}