package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
import org.canve.simpleGraph._

/*
 * a home for various vertices merger strategies
 */

trait MergeStrategies {
      
  /* 
   * Merges a list of vertices to its head vertex, collapsing all their
   * edges to the head vertex. 
   * 
   * Specifically the following steps are taken in order:
   * 
   * - remove all edges connecting between them
   * - re-wire all their edges to the head vertex
   * - remove them all, leaving only the head vertex 
   */
  
  // FIXME: this will not remove edges between given s2, s3 in List(s1,s2,s3)
  
  def reduceSymbols(extractedModel: ExtractedModel)(group: List[extractedModel.graph.Vertex]) = 
    group.reduce { (s1, s2) => 
      
      require(s1.data.symbolCompilerId != s2.data.symbolCompilerId)
      
      val ids = (s1.data.symbolCompilerId, s2.data.symbolCompilerId)
     
      extractedModel.graph -= extractedModel.graph edgesBetween(ids._1, ids._2)
      extractedModel.graph.vertexEdges(ids._2) foreach (e => 
        extractedModel.graph.edgeReWire(e, to = ids._1, from = ids._2)
      )
      extractedModel.graph -- ids._2
     
      s1
  }
}