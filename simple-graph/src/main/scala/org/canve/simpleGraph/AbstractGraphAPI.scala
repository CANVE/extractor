package org.canve.simpleGraph

/*
 * API definition
 */
abstract class AbstractGraph[VertexID, EdgeData, Vertex <: AbstractVertex[VertexID], Edge <: AbstractEdge[VertexID, EdgeData]]
  extends ExtraGraphAPI[VertexID, EdgeData, Vertex, Edge]
  with FilterableWalk[VertexID, EdgeData, Vertex, Edge]{

  /* Abstract methods required by an implementation */  
  
  def ++ (vertex: Vertex): AbstractGraph[VertexID, EdgeData, Vertex, Edge] 
  
  def ++ (edge: Edge): AbstractGraph[VertexID, EdgeData, Vertex, Edge] 
  
  def -= (vertex: Vertex): AbstractGraph[VertexID, EdgeData, Vertex, Edge] 
  
  def -= (edge: Edge): AbstractGraph[VertexID, EdgeData, Vertex, Edge] 
  
  def addIfNew (vertex: Vertex): AbstractGraph[VertexID, EdgeData, Vertex, Edge]
    
  def vertex(id: VertexID): Option[Vertex]
  
  def vertexEdges(id: VertexID): Set[Edge] 
  
  def vertexEdgePeer(id: VertexID, edge: Edge): VertexID
  
  def vertexIterator: Iterator[Vertex] // returns a new iterator every time called
  
  /* Concrete convenience methods for bulk addition and removal of vertices and edges */

  def ++ (input: Iterable[Addable]): AbstractGraph[VertexID, EdgeData, Vertex, Edge] = {
    input foreach {
      case v : AbstractVertex[VertexID]         => ++ (v.asInstanceOf[Vertex])
      case e : AbstractEdge[VertexID, EdgeData] => ++ (e.asInstanceOf[Edge])
    }
    this
  }
 
  def -= (inputs: Iterable[Addable]): AbstractGraph[VertexID, EdgeData, Vertex, Edge] = {
    inputs foreach {
      case v : AbstractVertex[VertexID]         => -= (v.asInstanceOf[Vertex])
      case e : AbstractEdge[VertexID, EdgeData] => -= (e.asInstanceOf[Edge])
    }
    this
  }
  
  def -= (inputs: Addable*): AbstractGraph[VertexID, EdgeData, Vertex, Edge] = this -= (inputs.toIterable)
  def += (inputs: Addable*): AbstractGraph[VertexID, EdgeData, Vertex, Edge] = this ++ (inputs.toIterable)
  
  def vertexCount: Int
  def edgeCount: Int
}

/*
 * A separate layer of graph API, that can be made optional to implement,
 * as the implementations mostly builds upon the pure api.
 */
abstract trait ExtraGraphAPI[VertexID, EdgeData, Vertex <: AbstractVertex[VertexID], Edge <: AbstractEdge[VertexID, EdgeData]] {
  self: AbstractGraph[VertexID, EdgeData, Vertex, Edge] => 

  def vertexEdgePeer(id: VertexID, edge: Edge): VertexID 

  def vertexEdgePeers(id: VertexID): Set[VertexID] // TODO: is this superfluous?!
  
  def vertexEdgePeersVerbose(id: VertexID): List[FilterFuncArguments[Vertex, Edge]]
  
  def edgeIterator: Iterator[Edge]
  
  def edgesBetween(v1: Vertex, v2:Vertex): Set[Edge]
  
  def edgeReWire(edge: Edge, from: VertexID, to:VertexID): SimpleGraph[VertexID, EdgeData, Vertex, Edge]
}

sealed abstract trait Addable

abstract trait AbstractVertex[VertexID] extends Addable {
  val data: Any // TODO: remove this member as superfluous in api usage?
  val key: VertexID  
} 

//TODO: remove the type parameter EdgeData if everything still works when data is defined here as Any
abstract trait AbstractEdge[VertexID, EdgeData] extends Addable {
  
  val data: EdgeData 
  val v1: VertexID
  val v2: VertexID
  lazy val dataCloneRef = data
  
  def edgeClone(newId1: VertexID = v1, newId2: VertexID = v2) = new AbstractEdge[VertexID, EdgeData] {
    val data = dataCloneRef
    val v1 = newId1
    val v2 = newId2
  }
}
