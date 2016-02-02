package org.canve

import org.canve.simpleGraph._
import org.canve.simpleGraph.algo.impl.GetPathsBetween
import tools.nsc.Global

package object compilerPlugin {
  
  case class ExtractionException(errorText: String) extends Exception(errorText)
  case class DataNormalizationException(errorText: String) extends Exception(errorText)
  
  type ExtractedSymbolRelation = String //(String, Option[TypeGraph])
  type SymbolCompilerId = Int
  type FurtherQualifiedID = String
  
  /*
   * extracted graph type
   */

  class ExtractedGraph extends 
    SimpleGraph
      [SymbolCompilerId, ExtractedSymbol, ExtractedSymbolRelation]((e: ExtractedSymbol) => e.symbolCompilerId)
  
  trait ContainsExtractedGraph { 
    val graph = 
      new ExtractedGraph
        with GetPathsBetween[SymbolCompilerId, ExtractedSymbol, ExtractedSymbolRelation] 
  }

  /*
   * Type graph type  
  
  type TypeID = Int
  class Type(id: TypeID, name: String)
  type TypeRelation = String
  type TypeGraph = SimpleGraph[TypeID, Type, TypeRelation]
   */
  
  /*
   * not really in use right now
   */
  
  object DataWarning {
    def apply(warning: String) {
      println(s"[canve data warning] $warning")
    }
  }
  
  def getUniqueName(global: Global)(s: global.Symbol): SymbolName = {

    (s.isAnonymousClass || 
        
     s.isAnonymousFunction ||

     s.nameString.startsWith("<local ") // could this rather use .isLocal*?
     
     ) match {
         case true =>
           DeAnonimizedName(global)(s) 
         case false => 
           new RealName(s.nameString)
    }
  }
}