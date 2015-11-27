package org.canve

import org.canve.simpleGraph.AbstractVertex
import org.canve.simpleGraph.AbstractEdge

package object compilerPlugin {
  
  case class ExtractionException(errorText: String) extends Exception(errorText)
  case class DataNormalizationException(errorText: String) extends Exception(errorText)
  
  type ExtractedSymbolRelation = String
  type SymbolCompilerId = Int
  
  /*
   * extracted graph type
   */

  type ManagedExtractedGraph = 
  org.canve.simpleGraph.SimpleGraph[
    SymbolCompilerId, 
    ExtractedSymbolRelation, 
    ManagedExtractedSymbol, 
    ManagedExtractedEdge
  ]
  
  case class ManagedExtractedSymbol(private val extractedSymbol: ExtractedSymbol) extends AbstractVertex[SymbolCompilerId] { 
    val key: SymbolCompilerId = extractedSymbol.symbolCompilerId
    val data = extractedSymbol
  }

  case class ManagedExtractedEdge(
    v1: SymbolCompilerId,
    data: ExtractedSymbolRelation,
    v2: SymbolCompilerId) 
      extends AbstractEdge[SymbolCompilerId, ExtractedSymbolRelation]
  
}