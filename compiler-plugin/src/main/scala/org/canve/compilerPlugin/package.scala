package org.canve

import org.canve.simpleGraph.AbstractVertex
import org.canve.simpleGraph.AbstractEdge

package object compilerPlugin {
  
  case class ManagedGraphNode(data: ExtractedSymbol) extends AbstractVertex[Int] { val key = data.id }    
  case class ManagedGraphEdge(id1: Int, id2: Int, data:String) extends AbstractEdge[Int, String]
  type ManagedGraph = org.canve.simpleGraph.SimpleGraph[Int, String, ManagedGraphNode, ManagedGraphEdge]
}