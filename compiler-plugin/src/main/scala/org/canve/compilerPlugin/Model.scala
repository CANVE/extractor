package org.canve.compilerPlugin
import tools.nsc.Global
import performance.Counters
import org.canve.simpleGraph.{AbstractVertex, AbstractEdge}

object Nodes {
  
  val existingCalls = Counters("existing node calls")
  
  var map: Map[Int, Node] = Map()

  def apply(global: Global)(s: global.Symbol): Node = {
    
    if (map.contains(s.id)) {
      existingCalls.increment
      map.get(s.id).get      
    }
    else
    {
      val newNode = s.sourceFile match {
        case null => // no source file included in this project for this entity 
          Node(s.id, s.nameString, s.kindString, !(s.isSynthetic), None, None)
        case _    => 
          Node(s.id, s.nameString, s.kindString, !(s.isSynthetic), SourceExtract(global)(s), Some(s.sourceFile.toString))
      }
      
      map += (s.id -> newNode)
      newNode
    }
  }
  
}

object Edges {
  
  val existingCalls = Counters("existing edge calls")
  
  var set: Set[Edge] = Set()
  
  def apply(id1: Int, edgeKind: String, id2: Int): Unit = {
    
    val edge = Edge(id1, edgeKind, id2)
    if (set.contains(edge)) 
      existingCalls.increment
    else
      set = set + edge
      
  } 
}

case class Edge
  (id1: Int,
   edgeKind: String,
   id2: Int) extends AbstractEdge[Int]
                
case class Node
  (id: Int,
   name: String,
   kind: String,
   notSynthetic: Boolean,
   source: Option[String],
   fileName: Option[String]) extends AbstractVertex[Int] {
  
  var ownersTraversed = false
  
}  

case class Graph(nodes: Set[Node], edges: Set[Edge])
