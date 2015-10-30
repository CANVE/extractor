package org.canve.simpleGraph

/*
 * cake-layer trait providing the ability to walk a graph with functional-style filters 
 */
trait FilterableWalk[ID, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID]] {
  self: AbstractGraph[ID, Vertex, Edge] =>
    
 /* 
  * returns a collection of vertex's edge peers, applying a @filter function 
  * 
  * a vertex's edges are filtered by a function that is free to perform any 
  * filtering logic based on the edge, and peer vertex properties  
  * 
  * @filter - function returning true if the vertex should pass through
  *  
  */
  def vertexEdgePeersFiltered(id: ID, filterFunc: FilterFuncArguments[Vertex, Edge] => Boolean): Set[ID] = {
    
    vertexEdgePeersVerbose(id)
      .filter(filterFunc)
      .map(_.peer.id)
      .toSet
  } 
}
