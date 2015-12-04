package org.canve

import org.canve.simpleGraph._
import org.canve.simpleGraph.algo.impl.GetPathsBetween
import tools.nsc.Global

package object compilerPlugin {
  
  case class ExtractionException(errorText: String) extends Exception(errorText)
  case class DataNormalizationException(errorText: String) extends Exception(errorText)
  
  type ExtractedSymbolRelation = String
  type SymbolCompilerId = Int
  type FurtherQualifiedID = String
  
  /*
   * extracted graph type
   */

  type ManagedExtractedGraph = SimpleGraph[SymbolCompilerId, ExtractedSymbol, ExtractedSymbolRelation]
  
  trait ContainsExtractedGraph { 
    val graph = 
      new ManagedExtractedGraph
        with GetPathsBetween[SymbolCompilerId, ExtractedSymbol, ExtractedSymbolRelation] 
  }
  
  def maybeName(global: Global)(s: global.Symbol): Option[String] = {
    (s.isAnonymousClass || s.isAnonymousFunction) match {
        case true => None
        case false => Some(s.nameString)   
    }
  }

}