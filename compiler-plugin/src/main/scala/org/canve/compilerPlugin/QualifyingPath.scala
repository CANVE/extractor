package org.canve.compilerPlugin
import tools.nsc.Global
import Util._

/*
 * A Qualified Identification of a symbol, that does not rely on its compiler assigned id, 
 * is useful for correlating symbols across project boundaries. Here we provide the necessary class 
 * for creating and using such an identification, which we will call for brevity QI.
 */

/* The class and factory */

case class QualifyingPath(value: List[QualificationUnit]) {
  
  /* 
   * if any anonymous object is part of the qualification chain, then (by definition) the
   * chain cannot be considered by any means unique - anonymous objects may sit side-by-side
   * under the same program context, and having no name, cannot be easily differentiated.
   */
  val fullyNamed: Boolean = value.forall(_.name.isDefined) 
  
  def pickle = value.map(kindAndName => "(" + kindAndName.kind + "|" + kindAndName.name + ")").mkString(".")
}

/*
 * companion object providing both a constructor from a global.symbol and 
 * a constructor from string-pickled input
 */
object QualifyingPath {

  def apply(s: String): QualifyingPath = 
    QualifyingPath(s.split('.').toList.map(s => 
      QualificationUnit(
        s.takeWhile(_!='|').drop(1), 
        deSerializeOption[String](s) match {
          case None => None
          case Some(s) => Some(s.dropWhile(_!='|').drop(1).dropRight(1))
        })))
  
  def apply(global: Global)(s: global.Symbol): QualifyingPath = {
    val kindNameList: List[QualificationUnit] = 
      s.ownerChain.reverse.map(owner => QualificationUnit(global)(owner))
   
    assert(kindNameList.head.kind == "package")
    assert(kindNameList.head.name.get == "<root>")
    
    QualifyingPath(kindNameList.drop(1))   
  }
}


/* auxiliary class and factory */

case class QualificationUnit(kind: String, name: Option[String])

object QualificationUnit {
  def apply(global: Global)(s: global.Symbol): QualificationUnit = {
    new QualificationUnit(s.kindString, maybeName(global)(s))
  }
}
