package org.canve.simpleGraph

/*
 * API definition
 */

trait GraphEntities[VertexID, VertexData, EdgeData] {
  
  sealed abstract trait Addable
  
  case class Vertex(key: VertexID, data: VertexData) extends Addable 
  
  case class Edge(
    var v1: VertexID,
    var v2: VertexID,  
    val data: EdgeData) extends Addable {
    
    def apply(v1: VertexID, data: EdgeData, v2: VertexID) = 
        new Edge(v1, v2, data)
  }
  
  /*
   * a type for a function that filters an edge walk step  
   * according to the edge and start vertex 
   */
  type WalkStepFilter[VertexID, Edge] = (VertexID, Edge) => Boolean
}



abstract class AbstractGraph[VertexID, VertexData, EdgeData]
  extends GraphEntities[VertexID, VertexData, EdgeData]
  with ExtraGraphAPI[VertexID, VertexData, EdgeData] {
  
  /* Abstract methods required by an implementation */  
  
  def ++ (vertex: Vertex): This
  
  def ++ (edge: Edge): This
  
  def -- (vertexId: VertexID): This 
  
  def -- (edge: Edge): This 
  
  def addIfNew (vertex: Vertex): This
    
  def vertex(id: VertexID): Option[Vertex]
  
  def vertexEdges(id: VertexID): Set[Edge] 
  
  def vertexEdgePeer(id: VertexID, edge: Edge): VertexID
  
  def vertexIterator: Iterator[Vertex] // returns a new iterator every time called
  
  /* Concrete convenience methods for bulk addition and removal of vertices and edges */

  def ++ (input: Iterable[Addable]): This = {
    input foreach {
      case v : Vertex => ++ (v.asInstanceOf[Vertex])
      case e : Edge   => ++ (e.asInstanceOf[Edge])
    }
    this
  }
 
  def -= (inputs: Iterable[Addable]): This = {
    inputs foreach {
      case v : Vertex => -- (v.key)
      case e : Edge   => -- (e.asInstanceOf[Edge])
    }
    this
  }
  
  def -- (inputs: Addable*): This = this -= (inputs.toIterable)
  def ++ (inputs: Addable*): This = this ++ (inputs.toIterable)
  
  def direction(id: VertexID, edge: Edge): EdgeDirection = { 
    (id == edge.v1, id == edge.v2) match {
      case (true, true)   => SelfLoop
      case (true, false)  => Egress
      case (false, true)  => Ingress
      case (false, false) => throw SimpleGraphApiException(s"vertex id $id is not part of edge supplied to function direction ($edge)")
    }
  }
  
  def vertexCount: Int
  def edgeCount: Int
}

/*
 * A separate layer of graph API, that can be made optional to implement,
 * as the implementations mostly builds upon the pure api.
 */
abstract trait ExtraGraphAPI[VertexID, VertexData, EdgeData] {
  self: AbstractGraph[VertexID, VertexData, EdgeData] => 

  type This = AbstractGraph[VertexID, VertexData, EdgeData]    
    
  def vertexEdgePeer(id: VertexID, edge: Edge): VertexID 

  def vertexEdgePeers(id: VertexID): Set[VertexID] // TODO: is this superfluous?!
  
  //def vertexEdgePeersVerbose(id: VertexID): List[FilterFuncArguments[Vertex, Edge]]
  
  def edgeIterator: Iterator[Edge]
  
  def edgesBetween(v1: VertexID, v2:VertexID): Set[Edge]
  
  def edgeReWire(edge: Edge, from: VertexID, to:VertexID): This
}

abstract sealed class EdgeDirection
object Ingress  extends EdgeDirection
object Egress   extends EdgeDirection
object SelfLoop extends EdgeDirection
