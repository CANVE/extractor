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

  object DataWarning {
    def apply(warning: String) {
      println(s"[canve data warning] $warning")
    }
  }
  
  def getUniqueName(global: Global)(s: global.Symbol): SymbolName = {

    (s.isAnonymousClass || 
        
     s.isAnonymousFunction ||
     
     s.nameString.startsWith("<local ") // as no method of global.Symbol seems to identify the same
     
     ) match {
         case true =>
           DeAnonimizedName(global)(s) 
         case false => 
           new RealName(s.nameString)
    }
  }
}