package org.canve.simpleGraph.algo
import org.canve.simpleGraph._

/*
 * a cache using type @VertexCache, 
 * for an algorithm to use against the given @graph
 */
case class AlgoCacheUnit[ID, EdgeData, VertexCacheUnit <: AbstractVertexCacheUnit, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID, EdgeData]]
  (cacheUnit: VertexCacheUnit, 
   graph: AbstractGraph[ID, EdgeData, Vertex, Edge]) { 
   
  private val vertexCacheIndex: Map[ID, AbstractVertexCacheUnit] = 
    graph.vertexIterator.map(vertex => (vertex.key, cacheUnit.apply)).toMap
  
  def apply(id: ID) = vertexCacheIndex.get(id)
}

abstract trait AbstractVertexCacheUnit {
  def apply: AbstractVertexCacheUnit
}

