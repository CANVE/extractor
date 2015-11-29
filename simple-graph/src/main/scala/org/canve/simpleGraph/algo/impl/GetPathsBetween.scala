package org.canve.simpleGraph.algo.impl

import org.canve.simpleGraph._
import org.canve.simpleGraph.algo._

/*
 * Algorithm implementation of finding all paths between two vertices,
 * sorted by path length.
 * 
 * Traverses starting from the origin vertex, visiting each vertex exactly once.
 * Backtracking from the target vertex, it stores a list of the paths reaching the 
 * target vertex in the cache of each vertex being part of such path.
 */

trait GetPathsBetween[VertexID, VertexData, EdgeData] {
  graph: AbstractGraph[VertexID, VertexData, EdgeData] => 

  /*
   * a per vertex cache type for this algorithm to use for its operation
   */
  class selfCacheUnit {
    var visited: Boolean = false
    var successPath: List[List[VertexID]] = List() 
  } 
  
  def getPathsBetween(
    origin: VertexID,
    target: VertexID,
    filterFunc: FilterFunc[VertexID, graph.Edge]): Option[List[List[VertexID]]] = {

    // a cache unit per vertex
    val cache: Map[VertexID, selfCacheUnit] = 
      graph.vertexIterator.map(vertex => (vertex.key, new selfCacheUnit)).toMap

    // assert basic validity of input
    if (graph.vertex(origin).isEmpty) throw SimpleGraphInvalidVertex("vertex with id $origin is not part of the graph provided to the algorithm")
    if (graph.vertex(target).isEmpty) throw SimpleGraphInvalidVertex("vertex with id $origin is not part of the graph provided to the algorithm")
    
    /*
     * The actual algorithm
     */
    def traverse(self: VertexID): Boolean = {

      val selfCache = cache(self)
      
      if (selfCache.visited) 
        return (selfCache.successPath.nonEmpty)
      else 
        selfCache.visited = true
        
      val vertexEdgePeers = graph.vertexEdges(self).filter(edge => filterFunc(self, edge)).map(edge => graph.vertexEdgePeer(self, edge))
      //println(self + ": " +  vertexEdgePeers)
        
      if (self == target) {
        selfCache.successPath = List(List(self))
        if (self != origin)
          return true   
      }
        
      selfCache.successPath = vertexEdgePeers.toList.flatMap(peer => traverse(peer) match {
        case true  => cache(peer).successPath.map(peerSuccessPath => List(self) ++ peerSuccessPath)
        case false => List()        
      })
      
      //println(selfCache.successPath)
      selfCache.successPath.filter(_.nonEmpty).nonEmpty
    }
    
    // translate algorithm's result into a desired form
    traverse(origin) match {
      case false => {
        assert(cache(origin).successPath.forall(_.isEmpty))
        None
      }
      case true => Some(cache(origin).successPath.toList.sortBy(_.length))
    }     
  }
}