package org.canve.scalaPlus

/*
 * Give key map semantics to scala map tuples (equivalent function and macro implementations).
 */
object KeyedMapTuple {
  
  /*
   * function implementation
   */
  def tupleKey[A, B](a: Tuple2[A,B]) = a._1
}

/*
 * Macro implementation - 2.11 upwards only
 */
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context 
object KeyedMapTupleMacro {
  def MapKey[Key, Val](tuple: Tuple2[Key, Val]): Key = macro MapKeyImpl[Key, Val]
  def MapKeyImpl[Key, Val](c: Context)(tuple: c.Expr[Tuple2[Key, Val]]): c.Expr[Key] = {
    import c.universe._
    reify { tuple.splice._1 }
  }
}