package org.canve.compilerPlugin

/*
 * Unique Symbol QualiferID, serializable
 */
case class KindAndName(kind: String, name: String)

case class QualifiedID(value: List[KindAndName]) {  
  def pickle = value.map(kindAndName => "(" + kindAndName.kind + "|" + kindAndName.name + ")").mkString(".")
}
object QualifiedID {
  def unpickle(s: String) = 
    QualifiedID(s.split('.').toList.map(pair => KindAndName(pair.takeWhile(_!='|').drop(1), pair.dropWhile(_!='|').drop(1).dropRight(1))))
}
