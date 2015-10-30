package org.canve.simpleGraph

/*
 * API definition
 */
abstract class AbstractGraph[ID, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID]]
  extends ExtraGraphAPI[ID, Vertex, Edge]
  with FilterableWalk[ID, Vertex, Edge]{
      
  def += (vertex: Vertex): AbstractGraph[ID, Vertex, Edge] 
  
  def += (edge: Edge): AbstractGraph[ID, Vertex, Edge] 
      
  def vertex(id: ID): Option[Vertex]
  
  def vertexEdges(id: ID): Set[Edge] 
  
  def vertexEdgePeer(id: ID, edge: Edge): ID
  
  def vertexIterator: Iterator[Vertex] // returns a new iterator every time called
  
  def += (inputs: Addable*): AbstractGraph[ID, Vertex, Edge] = {
    inputs.foreach(i => i match {
      case v : AbstractVertex[ID] => += (v.asInstanceOf[Vertex])
      case e : AbstractEdge[ID]   => += (e.asInstanceOf[Edge])
    })
    this
  }
}

abstract trait ExtraGraphAPI[ID, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID]] {
  self: AbstractGraph[ID, Vertex, Edge] => 

  def vertexEdgePeer(id: ID, edge: Edge): ID 

  def vertexEdgePeers(id: ID): Set[ID]
  
  def vertexEdgePeersVerbose(id: ID): List[FilterFuncArguments[Vertex, Edge]]
}

sealed abstract trait Addable

/*
 * trait to be mixed in by user code for making their nodes graph friendly
 */
abstract trait AbstractVertex[ID] extends Addable {  
  val id: ID 
} 

/*
 * trait to be mixed in by user code for making their edges graph friendly
*/
abstract trait AbstractEdge[ID] extends Addable {
  val id1: ID
  val id2: ID
}

