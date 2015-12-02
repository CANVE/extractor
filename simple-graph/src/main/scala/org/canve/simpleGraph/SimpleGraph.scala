package org.canve.simpleGraph
import collection.mutable.HashMap

/*
 * The concrete graph implementation
 * 
 * each edge is indexed in two indexes:
 * one is indexing by the first node,
 * the other by the second, so that all edges touching a node can be retrieved in O(1).
 */

class SimpleGraph[VertexID, VertexData, EdgeData] 
  extends AbstractGraph[VertexID, VertexData, EdgeData] {

  private val vertexIndex        = new HashMap[VertexID, Vertex]   
  protected val edgeIndex        = new EdgeIndex
  protected val reverseEdgeIndex = new EdgeIndex 
  
  /*
   * Single-direction edge index - own-implemented multimap. 
   */
  protected class EdgeIndex {
    
    private[SimpleGraph] val index = new HashMap[VertexID, Set[Edge]]
    
    def vertexEdges(id: VertexID): Option[Set[Edge]] = index.get(id)
    
    def add(key: VertexID, edge: Edge) = {
      index.get(key) match {
        case Some(set) => 
          set.contains(edge) match {
            case true  => throw SimpleGraphDuplicate(s"edge $edge already exists in edge index $this")
            case false => index.put(key, set + edge)
        }
        case None => index.put(key, Set(edge))
      }
    }
    
    // TODO: needs test code or switch to scala multimap
    def remove(key: VertexID, edge: Edge) = {
      index.get(key) match {
        case Some(set) => 
          set.contains(edge) match {
            case true =>
              val newSet: Set[Edge] = index.remove(key).get - edge
              if (!newSet.isEmpty) index.put(key, newSet)
            case false  => throw SimpleGraphDuplicate("edge $edge cannot be removed from edge index $this - it is not found in it")
          }
        case None => throw SimpleGraphDuplicate("edge $edge cannot be removed from edge index $this - it is not found in it")
      }
    }   
    
    def iterator = index.iterator
    
    def size = index.size
  }

  /*
   * public methods
   */
  
  def ++ (vertex: Vertex): SimpleGraph[VertexID, VertexData, EdgeData] = {    
      vertexIndex.get(vertex.key) match {      
      case Some(vertex) => throw SimpleGraphDuplicate("node with id $id already exists in the graph") 
      case None => vertexIndex += ((vertex.key, vertex)) // TODO: switch to put? 
      }
      this
  }

  // TODO: this is just a convenience wrapper, should probably not be part of the abstract api but rather an extra optional api trait
  def addIfNew (vertex: Vertex): SimpleGraph[VertexID, VertexData, EdgeData] = {    
    vertexIndex.get(vertex.key) match {
      case Some(vertex) =>  
      case None => vertexIndex += ((vertex.key, vertex)) // TODO: switch to put? 
    }
    this
  }
  
  def ++ (edge: Edge): SimpleGraph[VertexID, VertexData, EdgeData] = {
    List(edge.v1, edge.v2).foreach(id => 
      if (vertex(id).isEmpty) throw SimpleGraphInvalidEdge(s"will not add edge $edge because there is no vertex with id $id"))  
      
    edgeIndex.add(edge.v1, edge)
    reverseEdgeIndex.add(edge.v2, edge)  
    this
  }
  
  def addIfUnique (edge: Edge): SimpleGraph[VertexID, VertexData, EdgeData] = {
    List(edge.v1, edge.v2).foreach(id => 
      if (vertex(id).isEmpty) throw SimpleGraphInvalidEdge(s"will not add edge $edge because there is no vertex with id $id"))  
    
    if (vertexEdges(edge.v1).filter(_ == edge).isEmpty) { // efficient check for whether new edge is a duplicate one 
      edgeIndex.add(edge.v1, edge)
      reverseEdgeIndex.add(edge.v2, edge)
    } 
  
    this
  }
  
  // TODO: test code coverage
  def -- (vertexId: VertexID): SimpleGraph[VertexID, VertexData, EdgeData] = {   
    if (vertexEdges(vertexId).size < 0) throw SimpleGraphApiException(s"cannot remove vertex $vertexId as it still has one or more edges connected to it") 
    vertexIndex.get(vertexId) match {      
      case None => throw SimpleGraphInvalidVertex(s"node with id $vertexId cannot be removed from the graph - as it is not part of it")
      case Some(vertex) => vertexIndex -= vertexId
    }
    this
  }

  // TODO: test code coverage
  def -- (edge: Edge): SimpleGraph[VertexID, VertexData, EdgeData] = {
    edgeIndex.remove(edge.v1, edge)
    reverseEdgeIndex.remove(edge.v2, edge)
    this
  }
  
  /*
   * Switch this edge to point to vertex `to` wherever it currently points to `from`.
   * This method may thus change either of the edge's connections, or even both.
   */
  def edgeReWire(edge: Edge, from: VertexID, to:VertexID): SimpleGraph[VertexID, VertexData, EdgeData] = {

    /* replaces an edge's vertex connection, if needed */
    def replaceOrKeep(vertexId: VertexID): VertexID = {
      if (vertexId == from) to
      else vertexId
    }
    
    edge.v1 = replaceOrKeep(edge.v1)
    edge.v2 = replaceOrKeep(edge.v2)
    
    this
  }
  
  /* 
   * Get all edges directly connecting between given pair of vertices
   */
  def edgesBetween(v1: VertexID, v2:VertexID) : Set[Edge] = {
    if (v1 == v2) throw SimpleGraphApiException(s"pair of vertices provided is in fact the same vertex ($v1)") 
    vertexEdges(v1)
    .filter(edge => edge.v1 == v2 || edge.v2 == v2)
  }
  
  /* Returns the vertex having the given id, if such one exists in the graph */
  def vertex(id: VertexID): Option[Vertex] = vertexIndex.get(id)

  /* Efficiently returns all vertex edges */
  def vertexEdges(id: VertexID): Set[Edge] = {
    edgeIndex.vertexEdges(id).getOrElse(Set()) ++ 
    reverseEdgeIndex.vertexEdges(id).getOrElse(Set())
  } 

  def vertexCount = vertexIndex.size
  def edgeCount = edgeIndex.size

  /* Returns the opposite vertex of given vertex of edge */ 
  def vertexEdgePeer(id: VertexID, edge: Edge): VertexID = {
    if (id == edge.v1) return edge.v2
    if (id == edge.v2) return edge.v1
    throw SimpleGraphApiException(s"vertex with id $id is not part of the edge supplied")
  }
  
  /* Returns all vertices the given vertex is directly connected to */ 
  def vertexEdgePeers(id: VertexID): Set[VertexID] = {
    vertexEdges(id).map(edge => vertexEdgePeer(id, edge))
  }
  
  def vertexIterator: Iterator[Vertex] = vertexIndex.iterator.map(_._2)
  
  def edgeIterator: Iterator[Edge] = edgeIndex.iterator.flatMap(_._2)
  
}