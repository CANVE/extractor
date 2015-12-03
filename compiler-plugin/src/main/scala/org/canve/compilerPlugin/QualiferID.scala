package org.canve.compilerPlugin
import tools.nsc.Global

/*
 * Unique Symbol QualiferID, serializable
 */
case class KindAndName(kind: String, name: String)
object KindAndName {
  def apply(global: Global)(s: global.Symbol): KindAndName = {
    new KindAndName(s.kindString, s.nameString)
  }
}

case class QualifiedID(value: List[KindAndName]) {  
  def pickle = value.map(kindAndName => "(" + kindAndName.kind + "|" + kindAndName.name + ")").mkString(".")
}
object QualifiedID {
  
  def compute(global: Global)(s: global.Symbol): QualifiedID = {
    val kindNameList: List[KindAndName] = 
      s.ownerChain.reverse.map(owner => KindAndName(global)(owner))
   
    assert(kindNameList.head.kind == "package")
    assert(kindNameList.head.name == "<root>")

    /*
     * TODO: remove this commented out code
     * 
    (s.kindString == "method") match {
      case true => 
        val paramListList: List[List[global.Symbol]] = s.paramss
        QualifiedID(kindNameList.drop(1) ++ s.paramss.flatten.map(p => KindAndName(global)(p)))
      case false => 
        QualifiedID(kindNameList.drop(1))
    }
    */
    
    QualifiedID(kindNameList.drop(1))
    
  }
  
  def unpickle(s: String) = 
    QualifiedID(s.split('.').toList.map(pair => KindAndName(pair.takeWhile(_!='|').drop(1), pair.dropWhile(_!='|').drop(1).dropRight(1))))
}
