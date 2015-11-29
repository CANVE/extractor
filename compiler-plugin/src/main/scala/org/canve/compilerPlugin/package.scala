package org.canve

import org.canve.simpleGraph._

package object compilerPlugin {
  
  case class ExtractionException(errorText: String) extends Exception(errorText)
  case class DataNormalizationException(errorText: String) extends Exception(errorText)
  
  type ExtractedSymbolRelation = String
  type SymbolCompilerId = Int
  
  /*
   * extracted graph type
   */

  type ManagedExtractedGraph = 
    SimpleGraph[
      SymbolCompilerId, 
      ExtractedSymbol,
      ExtractedSymbolRelation
    ]
}