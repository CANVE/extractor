package org.canve.simpleGraph
import collection.mutable.HashMap

/*
 * The concrete graph implementation
 * 
 * each edge is indexed in two indexes:
 * one is indexing by the first node,
 * the other by the second, so that all edges touching a node can be retrieved in O(1).
 */

class SimpleGraph[ID, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID]] 
  extends AbstractGraph[ID, Vertex, Edge] {

  /*
   *  a custom constructor (because a companion object won't work 
   *  for a type parameterized class, so it seemed (http://stackoverflow.com/questions/32910646/custom-and-multiple-constructor-inheritance-in-scala) 
   */
  def this(vertices: Set[Vertex], edges: Set[Edge]) = {
    this
    vertices.foreach(this +=)
    edges.foreach(this +=)
  }
  
  private val vertexIndex    = new HashMap[ID, Vertex]   
  protected val edgeIndex      = new UnidirectionalEdgeIndex
  protected val reverseEdgeIndex = new UnidirectionalEdgeIndex 
  
  /*
   * Single-direction edge index 
   */
  protected class UnidirectionalEdgeIndex {
    
    private[SimpleGraph] val index = new HashMap[ID, Set[Edge]]
    
    def vertexEdges(id: ID): Option[Set[Edge]] = index.get(id)
    
    def addEdgeToUnidirectionalIndex(key: ID, edge: Edge) = {
      index.get(key) match {
        case Some(set) => 
          set.contains(edge) match {
            case true  =>
              throw SimpleGraphDuplicate("edge $edge already exists in the graph")
            case false => index.put(key, set + edge)
          }
        case None      => index.put(key, Set(edge))
      }
    }   
  }

  /*
   * public methods
   */

  def += (vertex: Vertex): SimpleGraph[ID, Vertex, Edge] = {    
    vertexIndex.get(vertex.id) match {      
      case Some(vertex) => throw SimpleGraphDuplicate("node with id $id already exists in the graph")
      case None => vertexIndex += ((vertex.id, vertex)) // TODO: switch to put?
    }
    this
  }
    
  def += (edge: Edge): SimpleGraph[ID, Vertex, Edge] = {

    List(edge.id1, edge.id2).foreach(id => 
      if (vertex(id).isEmpty) throw SimpleGraphInvalidEdge("will not add edge $edge because there is no vertex with id $id"))  
    
    edgeIndex.addEdgeToUnidirectionalIndex(edge.id1, edge)  
    reverseEdgeIndex.addEdgeToUnidirectionalIndex(edge.id2, edge)  
    this
  }
  
  def vertex(id: ID): Option[Vertex] = vertexIndex.get(id)
  
  def vertexEdges(id: ID): Set[Edge] = {
    edgeIndex.vertexEdges(id).getOrElse(Set()) ++ 
    reverseEdgeIndex.vertexEdges(id).getOrElse(Set())
  } 

  // TODO: test cases, add to abstract class
  def vertexEdgePeer(id: ID, edge: Edge): ID = {
    if (id == edge.id1) return edge.id2
    if (id == edge.id2) return edge.id1
    throw SimpleGraphApiException("vertex with id $id is not part of the edge supplied")
  }
  
  // TODO: test cases, add to abstract class, then create efficient version
  def vertexEdgePeers(id: ID): Set[ID] = {
    vertexEdges(id).map(edge => vertexEdgePeer(id, edge))
  }
  
  def vertexIterator: Iterator[Vertex] = vertexIndex.iterator.map(_._2)
  
  def vertexEdgePeersVerbose(id: ID): List[FilterFuncArguments[Vertex, Edge]] = { 
    edgeIndex
      .vertexEdges(id).getOrElse(Set()).toList
      .map(edge => FilterFuncArguments(Egress, edge, vertex(edge.id2).get)) ++
    reverseEdgeIndex     
      .vertexEdges(id).getOrElse(Set()).toList
      .map(edge => FilterFuncArguments(Ingress, edge, vertex(edge.id1).get))
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