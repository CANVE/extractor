package org.canve.compilerPlugin
import tools.nsc.Global

/*
 * Unique Symbol QualiferID, serializable
 */
case class KindAndName(kind: String, name: String)

case class QualifiedID(value: List[KindAndName]) {  
  def pickle = value.map(kindAndName => "(" + kindAndName.kind + "|" + kindAndName.name + ")").mkString(".")
}
object QualifiedID {
  
   def compute(global: Global)(s: global.Symbol) = {
     val kindNameList: List[KindAndName] = s.ownerChain.reverse.map(owner => KindAndName(owner.kindString, owner.nameString))
     assert(kindNameList.head.kind == "package")
     assert(kindNameList.head.name == "<root>")
     QualifiedID(kindNameList.drop(1))
   }
  
  def unpickle(s: String) = 
    QualifiedID(s.split('.').toList.map(pair => KindAndName(pair.takeWhile(_!='|').drop(1), pair.dropWhile(_!='|').drop(1).dropRight(1))))
}
