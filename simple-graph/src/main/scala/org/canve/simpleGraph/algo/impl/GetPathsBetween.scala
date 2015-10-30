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
 * 
 */
case class GetPathsBetween[ID, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID]]
  (graph: AbstractGraph[ID, Vertex, Edge], 
   filterFunc: FilterFuncArguments[Vertex, Edge] => Boolean, 
   origin: ID,
   target: ID) extends GraphAlgo {
    
    var neverRun = true
  
    /*
     * a per vertex cache type for this algorithm to use for its operation
     */
    protected class selfCacheUnit {
      var visited: Boolean = false
      var successPath: List[List[ID]] = List() 
    } 
  
    private val cache: Map[ID, selfCacheUnit] = 
      graph.vertexIterator.map(vertex => (vertex.id, new selfCacheUnit)).toMap
      
    private def traverse(self: ID): Boolean = {
      //println(self)
      val selfCache = cache(self)
      
      if (selfCache.visited) 
        return (selfCache.successPath.nonEmpty)
      else 
        selfCache.visited = true
        
      val vertexEdgePeers = graph.vertexEdgePeersFiltered(self, filterFunc)
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
    
    def run: Option[List[List[ID]]] = {
      
      if (!neverRun) throw SimpleGraphAlgoException("GraphAlgo object $this already run")  
      
      neverRun = false
      
      if (graph.vertex(origin).isEmpty) throw SimpleGraphInvalidVertex("vertex with id $origin is not part of the graph provided to the algorithm")
      if (graph.vertex(target).isEmpty) throw SimpleGraphInvalidVertex("vertex with id $origin is not part of the graph provided to the algorithm")
      
      traverse(origin) match {
        case false => {
          assert(cache(origin).successPath.forall(_.isEmpty))
          None
        }
        case true => Some(cache(origin).successPath.toList.sortBy(_.length))
      }     
    }
}