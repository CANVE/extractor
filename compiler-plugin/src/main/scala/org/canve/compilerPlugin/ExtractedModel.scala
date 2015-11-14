package org.canve.compilerPlugin
import tools.nsc.Global
import performance.Counters
import org.canve.simpleGraph.{AbstractVertex, AbstractEdge}

/*
 * a class representing a single and complete model extracted for the project being compiled, 
 * comprising symbol details and symbol relations 
 */
class ExtractedGraph {
  val extractedSymbols = new ExtractedSymbols
  val extractedSymbolRelations = new ExtractedSymbolRelations
}

/*
 * an extracted symbols collection (singleton as we run once per project)
 */
class ExtractedSymbols {
  
  val existingCalls = Counters("existing node calls")
  
  private var map: Map[Int, ExtractedSymbol] = Map()
  
  def apply(global: Global)(s: global.Symbol): ExtractedSymbol = {
    
    if (map.contains(s.id)) {
      existingCalls.increment
      map.get(s.id).get      
    }
    else
    {
      val kindNameList: List[KindAndName] = s.ownerChain.reverse.map(owner => KindAndName(owner.kindString, owner.nameString))
      assert(kindNameList.head.kind == "package")
      assert(kindNameList.head.name == "<root>")
      val qualifiedId = QualifiedID(kindNameList.drop(1))
      
      val newNode = s.sourceFile match {
        case null => // no source file included in this project for this entity 
          ExtractedSymbol(s.id, s.nameString, s.kindString, !(s.isSynthetic), qualifiedId, ExternallyDefined, None, None)
        case _    => 
          ExtractedSymbol(s.id, s.nameString, s.kindString, !(s.isSynthetic), qualifiedId, ProjectDefined, Some(s.sourceFile.toString), SourceExtract(global)(s))
      }
      
      map += (s.id -> newNode)
      newNode
    }
  }
  
  def size = map.size

  def get = map.map(_._2)
  
}

/*
 * an objects collection (singleton as we run once per project)
 */
class ExtractedSymbolRelations {
  
  val existingCalls = Counters("existing edge calls")
  
  private var set: Set[ExtractedSymbolRelation] = Set()
  
  def apply(id1: Int, edgeKind: String, id2: Int): Unit = {
    
    val edge = ExtractedSymbolRelation(id1, edgeKind, id2)
    if (set.contains(edge)) 
      existingCalls.increment
    else
      set = set + edge

  }
  
  def get = set
  
  def size = set.size
}