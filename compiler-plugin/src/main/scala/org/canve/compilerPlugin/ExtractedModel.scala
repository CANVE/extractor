package org.canve.compilerPlugin
import tools.nsc.Global
import performance._
import org.canve.simpleGraph._

/*
 * a class representing a single and complete model extracted for the project being compiled, 
 * comprising symbol details and symbol relations 
 */
class ExtractedModel(global: Global) {
 
  val TraversalSymbolRevisit = Counter("TraversalSymbolRevisit")
  
  val graph = new ManagedExtractedGraph
  val codes = new ExtractedCodes
  
  def addOrGet(global: Global)(s: global.Symbol): ExtractedSymbol = {
    graph.vertex(s.id) match {
      case Some(v: graph.Vertex) => v.data 
      case None => add(global)(s)
    }
  }
  
  def add(global: Global)(s: global.Symbol): ExtractedSymbol = {
    graph.vertex(s.id) match {
      case Some(v: graph.Vertex) =>
        //throw ExtractionException(s"graph already has symbol with id ${s.id}")
        TraversalSymbolRevisit.increment
        v.data
      case None =>
        val qualifiedId = QualifiedID.compute(global)(s)
  
        /*
         * determine whether the symbol at hand is defined in the current project, 
         */ 
        val definingProject = s.sourceFile match {
          case null => ExternallyDefined // no source file included in this project for this entity
          case _    => ProjectDefined
        }
  

        /*
         * add the symbol to the extracted model
         */
        val extractedSymbol = ExtractedSymbol(s.id, s.nameString, s.kindString, !(s.isSynthetic), qualifiedId, definingProject)
        graph ++ graph.Vertex(extractedSymbol.symbolCompilerId, extractedSymbol) 
            
        /*
         * pass on to attempt to extract the symbol's source code, 
         * if it is defined in the current project 
         */
        definingProject match {
          case ProjectDefined    => codes(global)(s, AttemptCodeExtract(global)(s))
          case ExternallyDefined => // no source file included in this project for this entity
        }
        
        normalization.CompleteOwnerChain(global)(extractedSymbol, s, this)
        extractedSymbol
    }
  }
  
  def add(symbolCompilerId1: SymbolCompilerId, edgeKind: ExtractedSymbolRelation, symbolCompilerId2: SymbolCompilerId) = {
    graph addIfUnique graph.Edge(symbolCompilerId1, symbolCompilerId2, edgeKind)
  }
  
  def addIfUnique(symbolCompilerId1: SymbolCompilerId, relation: ExtractedSymbolRelation, symbolCompilerId2: SymbolCompilerId) = {
    graph addIfUnique graph.Edge(symbolCompilerId1, symbolCompilerId2, relation)
  }
}

class ExtractedCodes {
  
  var map: Map[Int, ExtractedCode] = Map()
  
  def apply(global: Global)(s: global.Symbol, code: ExtractedCode) = {
    map.contains(s.id) match {
      case false => map += (s.id -> code)
      case true  => // do nothing 
    }
  }
  
  def get = map
}