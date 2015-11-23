package org.canve.simpleGraph
import collection.mutable.HashMap

/*
 * The concrete graph implementation
 * 
 * each edge is indexed in two indexes:
 * one is indexing by the first node,
 * the other by the second, so that all edges touching a node can be retrieved in O(1).
 */

class SimpleGraph[VertexID, EdgeData, Vertex <: AbstractVertex[VertexID], Edge <: AbstractEdge[VertexID, EdgeData]] 
  extends AbstractGraph[VertexID, EdgeData, Vertex, Edge] {

  /*
   *  a custom constructor (because a companion object won't work 
   *  for a type parameterized class, so it seemed (http://stackoverflow.com/questions/32910646/custom-and-multiple-constructor-inheritance-in-scala) 
   */
  def this(vertices: Set[Vertex], edges: Set[Edge]) = {
    this
    vertices.foreach(this +=)
    edges.foreach(this +=)
  }
  
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
    
    def size = index.size
  }

  /*
   * public methods
   */
  
  def += (vertex: Vertex): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {    
      vertexIndex.get(vertex.key) match {      
      case Some(vertex) => throw SimpleGraphDuplicate("node with id $id already exists in the graph") 
      case None => vertexIndex += ((vertex.key, vertex)) // TODO: switch to put? 
      }
      this
  }

  // TODO: this is just a convenience wrapper, should probably not be part of the abstract api but rather an extra optional api trait
  def addIfNew(vertex: Vertex): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {    
    vertexIndex.get(vertex.key) match {
      case Some(vertex) =>  
      case None => vertexIndex += ((vertex.key, vertex)) // TODO: switch to put? 
    }
    this
  }
  
  def += (edge: Edge): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {
    List(edge.node1, edge.node2).foreach(id => 
      if (vertex(id).isEmpty) throw SimpleGraphInvalidEdge(s"will not add edge $edge because there is no vertex with id $id"))  
    
    edgeIndex.add(edge.node1, edge)
    reverseEdgeIndex.add(edge.node2, edge)  
    this
  }
  
  // TODO: test code coverage
  def -= (vertexId: VertexID): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {   
    if (vertexEdges(vertexId).size < 0) throw SimpleGraphApiException(s"cannot remove vertex $vertexId as it still has one or more edges connected to it") 
    vertexIndex.get(vertexId) match {      
      case None => throw SimpleGraphInvalidVertex(s"node with id $vertexId cannot be removed from the graph - as it is not part of it")
      case Some(vertex) => vertexIndex -= vertex.key 
    }
    this
  }

  // TODO: test code coverage
  def -= (edge: Edge): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {
    edgeIndex.remove(edge.node1, edge)
    reverseEdgeIndex.remove(edge.node2, edge)
    this
  }
  
  // TODO: test code coverage
  def edgeReWire(edge: Edge, from: VertexID, to:VertexID): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {

    // remove
    this -= edge
    
    /* 
     * utility function for switching a vertex the edge is connected to if required -
     * only if the vertex pointed at is the one that needs replacing
     */
    def maybeReplace(id: VertexID) = {
      (id == from) match { 
        case true  => to  
        case false => id
      }
    }
    
    // add back - now connected to new vertex as needed
    this += edge.edgeClone(newId1 = maybeReplace(edge.node1), 
                           newId2 = maybeReplace(edge.node2))
    this
  }
  
  def vertex(id: VertexID): Option[Vertex] = vertexIndex.get(id)
  
  def vertexEdges(id: VertexID): Set[Edge] = {
    edgeIndex.vertexEdges(id).getOrElse(Set()) ++ 
    reverseEdgeIndex.vertexEdges(id).getOrElse(Set())
  } 

  def vertexCount = vertexIndex.size
  def edgeCount = edgeIndex.size
  
  // TODO: test cases, add to abstract class
  def vertexEdgePeer(id: VertexID, edge: Edge): VertexID = {
    if (id == edge.node1) return edge.node2
    if (id == edge.node2) return edge.node1
    throw SimpleGraphApiException(s"vertex with id $id is not part of the edge supplied")
  }
  
  // TODO: test cases, add to abstract class, then create efficient version
  def vertexEdgePeers(id: VertexID): Set[VertexID] = {
    vertexEdges(id).map(edge => vertexEdgePeer(id, edge))
  }
  
  def vertexIterator: Iterator[Vertex] = vertexIndex.iterator.map(_._2)
  
  def vertexEdgePeersVerbose(id: VertexID): List[FilterFuncArguments[Vertex, Edge]] = { 
    edgeIndex
      .vertexEdges(id).getOrElse(Set()).toList
      .map(edge => FilterFuncArguments(Egress, edge, vertex(edge.node2).get)) ++
    reverseEdgeIndex     
      .vertexEdges(id).getOrElse(Set()).toList
      .map(edge => FilterFuncArguments(Ingress, edge, vertex(edge.node1).get))
  }
}

/*
/*
 * companion object / constructors
 */
object SimpleGraph {
  
  // code credit: http://stackoverflow.com/questions/32768816/bypassing-sets-invariance-in-scala/32769068#32769068
  def apply[ID, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID]]
           (vertices: Set[Vertex], edges: Set[Edge]) = {
    
    val simpleGraph = new SimpleGraph[ID, Vertex, Edge]
    vertices.foreach(simpleGraph +=)
    edges.foreach(simpleGraph +=)
    simpleGraph
  }
  
}
*/