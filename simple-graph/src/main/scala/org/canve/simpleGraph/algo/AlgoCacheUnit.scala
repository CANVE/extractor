package org.canve.simpleGraph.algo
import org.canve.simpleGraph._

/*
 * a cache using type @VertexCache, 
 * for an algorithm to use against the given @graph
 */
case class AlgoCacheUnit[VertexID, EdgeData, VertexCacheUnit <: AbstractVertexCacheUnit, Vertex <: AbstractVertex[VertexID], Edge <: AbstractEdge[VertexID, EdgeData]]
  (cacheUnit: VertexCacheUnit, 
   graph: AbstractGraph[VertexID, EdgeData, Vertex, Edge]) { 
   
  private val vertexCacheIndex: Map[VertexID, AbstractVertexCacheUnit] = 
    graph.vertexIterator.map(vertex => (vertex.key, cacheUnit.apply)).toMap
  
  def apply(id: VertexID) = vertexCacheIndex.get(id)
}

abstract trait AbstractVertexCacheUnit {
  def apply: AbstractVertexCacheUnit
}

