package org.canve.simpleGraph

/*
 * API definition
 */
abstract class AbstractGraph[VertexID, EdgeData, Vertex <: AbstractVertex[VertexID], Edge <: AbstractEdge[VertexID, EdgeData]]
  extends ExtraGraphAPI[VertexID, EdgeData, Vertex, Edge]
  with FilterableWalk[VertexID, EdgeData, Vertex, Edge]{
      
  def += (vertex: Vertex): AbstractGraph[VertexID, EdgeData, Vertex, Edge] 
  
  def += (edge: Edge): AbstractGraph[VertexID, EdgeData, Vertex, Edge] 
  
  def -= (vertexId: VertexID): AbstractGraph[VertexID, EdgeData, Vertex, Edge] 
  
  def -= (edge: Edge): AbstractGraph[VertexID, EdgeData, Vertex, Edge] 
  
  def addIfNew (vertex: Vertex): AbstractGraph[VertexID, EdgeData, Vertex, Edge]
    
  def vertex(id: VertexID): Option[Vertex]
  
  def vertexEdges(id: VertexID): Set[Edge] 
  
  def vertexEdgePeer(id: VertexID, edge: Edge): VertexID
  
  def vertexIterator: Iterator[Vertex] // returns a new iterator every time called
  
  /*
   * Convenience method for bulk addition of both vertices and edges
   */
  def += (inputs: Addable*): AbstractGraph[VertexID, EdgeData, Vertex, Edge] = {
    inputs.foreach(i => i match {
      case v : AbstractVertex[VertexID]         => += (v.asInstanceOf[Vertex])
      case e : AbstractEdge[VertexID, EdgeData] => += (e.asInstanceOf[Edge])
    })
    this
  }
  
  def vertexCount: Int
  def edgeCount: Int
}

abstract trait ExtraGraphAPI[VertexID, EdgeData, Vertex <: AbstractVertex[VertexID], Edge <: AbstractEdge[VertexID, EdgeData]] {
  self: AbstractGraph[VertexID, EdgeData, Vertex, Edge] => 

  def vertexEdgePeer(id: VertexID, edge: Edge): VertexID 

  def vertexEdgePeers(id: VertexID): Set[VertexID] // TODO: is this superfluous?!
  
  def vertexEdgePeersVerbose(id: VertexID): List[FilterFuncArguments[Vertex, Edge]]
  
  def edgeIterator: Iterator[Edge]
}

sealed abstract trait Addable

abstract trait AbstractVertex[VertexID] extends Addable {
  val data: Any // TODO: remove this member as superfluous in api usage?
  val key: VertexID  
} 

//TODO: remove the type parameter EdgeData if everything still works when data is defined here as Any
abstract trait AbstractEdge[VertexID, EdgeData] extends Addable {
  
  val data: EdgeData 
  val node1: VertexID
  val node2: VertexID
  lazy val dataCloneRef = data
  
  def edgeClone(newId1: VertexID = node1, newId2: VertexID = node2) = new AbstractEdge[VertexID, EdgeData] {
    val data = dataCloneRef
    val node1 = newId1
    val node2 = newId2
  }
}
