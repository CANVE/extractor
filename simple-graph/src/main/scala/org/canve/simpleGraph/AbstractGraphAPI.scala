package org.canve.simpleGraph

/*
 * API definition
 */
abstract class AbstractGraph[ID, EdgeData, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID, EdgeData]]
  extends ExtraGraphAPI[ID, EdgeData, Vertex, Edge]
  with FilterableWalk[ID, EdgeData, Vertex, Edge]{
      
  def += (vertex: Vertex): AbstractGraph[ID, EdgeData, Vertex, Edge] 
  
  def += (edge: Edge): AbstractGraph[ID, EdgeData, Vertex, Edge] 
  
  def -= (vertexId: ID): AbstractGraph[ID, EdgeData, Vertex, Edge] 
  
  def -= (edge: Edge): AbstractGraph[ID, EdgeData, Vertex, Edge] 
  
  def addIfNew (vertex: Vertex): AbstractGraph[ID, EdgeData, Vertex, Edge]
    
  def vertex(id: ID): Option[Vertex]
  
  def vertexEdges(id: ID): Set[Edge] 
  
  def vertexEdgePeer(id: ID, edge: Edge): ID
  
  def vertexIterator: Iterator[Vertex] // returns a new iterator every time called
  
  /*
   * Convenience method for bulk addition of both vertices and edges
   */
  def += (inputs: Addable*): AbstractGraph[ID, EdgeData, Vertex, Edge] = {
    inputs.foreach(i => i match {
      case v : AbstractVertex[ID]         => += (v.asInstanceOf[Vertex])
      case e : AbstractEdge[ID, EdgeData] => += (e.asInstanceOf[Edge])
    })
    this
  }
}

abstract trait ExtraGraphAPI[ID, EdgeData, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID, EdgeData]] {
  self: AbstractGraph[ID, EdgeData, Vertex, Edge] => 

  def vertexEdgePeer(id: ID, edge: Edge): ID 

  def vertexEdgePeers(id: ID): Set[ID]
  
  def vertexEdgePeersVerbose(id: ID): List[FilterFuncArguments[Vertex, Edge]]
}

sealed abstract trait Addable

abstract trait AbstractVertex[ID] extends Addable {
  val data: Any
  val key: ID  
} 

//TODO: remove the type parameter EdgeData if everything still works when data is defined here as Any
abstract trait AbstractEdge[ID, EdgeData] extends Addable {
  
  val data: EdgeData 
  val id1: ID
  val id2: ID
  lazy val dataSynonym = data
  
  def edgeClone(newId1: ID = id1, newId2: ID = id2) = new AbstractEdge[ID, EdgeData] {
    val data = dataSynonym
    val id1 = newId1
    val id2 = newId2
  }
}
