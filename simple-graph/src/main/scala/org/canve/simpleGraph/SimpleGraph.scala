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
    vertices.foreach(this ++)
    edges.foreach(this ++)
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
    
    def iterator = index.iterator
    
    def size = index.size
  }

  /*
   * public methods
   */
  
  def ++ (vertex: Vertex): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {    
      vertexIndex.get(vertex.key) match {      
      case Some(vertex) => throw SimpleGraphDuplicate("node with id $id already exists in the graph") 
      case None => vertexIndex += ((vertex.key, vertex)) // TODO: switch to put? 
      }
      this
  }

  // TODO: this is just a convenience wrapper, should probably not be part of the abstract api but rather an extra optional api trait
  def addIfNew (vertex: Vertex): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {    
    vertexIndex.get(vertex.key) match {
      case Some(vertex) =>  
      case None => vertexIndex += ((vertex.key, vertex)) // TODO: switch to put? 
    }
    this
  }
  
  def ++ (edge: Edge): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {
    List(edge.v1, edge.v2).foreach(id => 
      if (vertex(id).isEmpty) throw SimpleGraphInvalidEdge(s"will not add edge $edge because there is no vertex with id $id"))  
      
    edgeIndex.add(edge.v1, edge)
    reverseEdgeIndex.add(edge.v2, edge)  
    this
  }
  
  def addIfUnique (edge: Edge): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {
    List(edge.v1, edge.v2).foreach(id => 
      if (vertex(id).isEmpty) throw SimpleGraphInvalidEdge(s"will not add edge $edge because there is no vertex with id $id"))  
    
    vertexEdgePeersFiltered(
      edge.v1, { 
        f: FilterFuncArguments[Vertex, Edge] => f.peer.key == edge.v2 && f.edge.data == edge.data
      }).isEmpty match {
        case true  => 
          edgeIndex.add(edge.v1, edge)
          reverseEdgeIndex.add(edge.v2, edge)
        case false  => // do nothing 
      }
    this
  }
  
  // TODO: test code coverage
  def -= (vertex: Vertex): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {   
    if (vertexEdges(vertex.key).size < 0) throw SimpleGraphApiException(s"cannot remove vertex $vertex as it still has one or more edges connected to it") 
    vertexIndex.get(vertex.key) match {      
      case None => throw SimpleGraphInvalidVertex(s"node with id $vertex cannot be removed from the graph - as it is not part of it")
      case Some(vertex) => vertexIndex -= vertex.key 
    }
    this
  }

  // TODO: test code coverage
  def -= (edge: Edge): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {
    edgeIndex.remove(edge.v1, edge)
    reverseEdgeIndex.remove(edge.v2, edge)
    this
  }
  
  // TODO: test code coverage
  def edgeReWire(edge: Edge, from: VertexID, to:VertexID): SimpleGraph[VertexID, EdgeData, Vertex, Edge] = {

    // remove the edge from this graph
    this -= edge
    
    /* 
     * utility function for switching a vertex the edge is connected to -
     * in the case that the vertex pointed at is the one that needs replacing
     */
    def replaceOrKeep(id: VertexID) = {
      (id == from) match { 
        case true  => to  
        case false => id
      }
    }
    
    // add back - now connected to new vertex as needed
    this += edge.edgeClone(newId1 = replaceOrKeep(edge.v1), 
                           newId2 = replaceOrKeep(edge.v2))
    this
  }
  
  def edgesBetween(v1: Vertex, v2:Vertex) : Set[Edge] = {
    this.vertexEdges(v1.key).filter(e => e.v1 == v1 || e.v2 == v2)
      
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
    if (id == edge.v1) return edge.v2
    if (id == edge.v2) return edge.v1
    throw SimpleGraphApiException(s"vertex with id $id is not part of the edge supplied")
  }
  
  // TODO: test cases, add to abstract class, then create efficient version
  def vertexEdgePeers(id: VertexID): Set[VertexID] = {
    vertexEdges(id).map(edge => vertexEdgePeer(id, edge))
  }
  
  def vertexIterator: Iterator[Vertex] = vertexIndex.iterator.map(_._2)
  
  def edgeIterator: Iterator[Edge] = edgeIndex.iterator.flatMap(_._2)
  
  def vertexEdgePeersVerbose(id: VertexID): List[FilterFuncArguments[Vertex, Edge]] = { 
    edgeIndex
      .vertexEdges(id).getOrElse(Set()).toList
      .map(edge => FilterFuncArguments(Egress, edge, vertex(edge.v2).get)) ++
    reverseEdgeIndex     
      .vertexEdges(id).getOrElse(Set()).toList
      .map(edge => FilterFuncArguments(Ingress, edge, vertex(edge.v1).get))
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