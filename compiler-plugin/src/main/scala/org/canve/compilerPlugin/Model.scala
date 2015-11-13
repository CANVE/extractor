package org.canve.compilerPlugin
import tools.nsc.Global
import performance.Counters
import org.canve.simpleGraph.{AbstractVertex, AbstractEdge}

/*
 * the extracted symbols collection (singleton as we run once per project)
 */
object ExtractedSymbols {
  
  val existingCalls = Counters("existing node calls")
  
  var map: Map[Int, ExtractedSymbol] = Map()

  def apply(global: Global)(s: global.Symbol): ExtractedSymbol = {
    
    if (map.contains(s.id)) {
      existingCalls.increment
      map.get(s.id).get      
    }
    else
    {
      val qualifiedId = s.ownerChain.map(_.nameString).mkString(".") + "." + s.kindString
      
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
  
}

/*
 * the objects collection (singleton as we run once per project)
 */
object ExtractedSymbolRelations {
  
  val existingCalls = Counters("existing edge calls")
  
  var set: Set[ExtractedSymbolRelation] = Set()
  
  def apply(id1: Int, edgeKind: String, id2: Int): Unit = {
    
    val edge = ExtractedSymbolRelation(id1, edgeKind, id2)
    if (set.contains(edge)) 
      existingCalls.increment
    else
      set = set + edge
      
  } 
}

/*
 * the edge class
 */
case class ExtractedSymbolRelation
  (symbolID1: Int,
   relation: String,
   symbolID2: Int) 

/*
 * types signifying whether a symbol is defined in 
 * the current project, or is an external one
 */
abstract class DefiningProject
object ProjectDefined extends DefiningProject
object ExternallyDefined extends DefiningProject

/*
 * The node class
 */
case class ExtractedSymbol 
  (id: Int,
  name: String,
  kind: String,
  notSynthetic: Boolean,
  qualifiedId: String,
  definingProject: DefiningProject,
  definingFileName: Option[String],
  sourceCode: Option[String]) 
  extends NodeSerialization {
    var ownersTraversed = false 
}

case class Graph(nodes: Set[ExtractedSymbol], edges: Set[ExtractedSymbolRelation])
