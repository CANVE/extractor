package org.canve.compilerPlugin
import tools.nsc.Global
import performance._
import org.canve.simpleGraph.{AbstractVertex, AbstractEdge}

/*
 * a class representing a single and complete model extracted for the project being compiled, 
 * comprising symbol details and symbol relations 
 */
class ExtractedModel(global: Global) {
 
  val TraversalSymbolRevisit = Counter("TraversalSymbolRevisit")
  
  val graph = new ManagedExtractedGraph
  val codes = new ExtractedCodes
  
  def addOrGet(global: Global)(s: global.Symbol): ManagedExtractedSymbol = {
    graph.vertex(s.id) match {
      case Some(managedExtractedSymbol: ManagedExtractedSymbol) => managedExtractedSymbol 
      case None    => add(global)(s)
    }
  }
  
  def add(global: Global)(s: global.Symbol): ManagedExtractedSymbol = {
    graph.vertex(s.id) match {
      case Some(managedExtractedSymbol: ManagedExtractedSymbol) =>
        //throw ExtractionException(s"graph already has symbol with id ${s.id}")
        TraversalSymbolRevisit.increment
        managedExtractedSymbol
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
        val managedExtractedSymbol = {
          val extractedSymbol = ExtractedSymbol(s.id, s.nameString, s.kindString, !(s.isSynthetic), qualifiedId, definingProject)
          ManagedExtractedSymbol(extractedSymbol) 
        }

        graph += managedExtractedSymbol
              
        /*
         * pass on to attempt to extract the symbol's source code, 
         * if it is defined in the current project 
         */
        definingProject match {
          case ProjectDefined    => codes(global)(s, AttemptCodeExtract(global)(s))
          case ExternallyDefined => // no source file included in this project for this entity
        }
        
        normalization.CompleteOwnerChain(global)(managedExtractedSymbol, s, this)
        managedExtractedSymbol
    }
  }
  
  def add(symbolCompilerId1: SymbolCompilerId, edgeKind: ExtractedSymbolRelation, symbolCompilerId2: SymbolCompilerId) = {
    graph addIfUnique ManagedExtractedEdge(symbolCompilerId1,edgeKind,symbolCompilerId2)
  }
  
  def addIfUnique(symbolCompilerId1: SymbolCompilerId, relation: ExtractedSymbolRelation, symbolCompilerId2: SymbolCompilerId) = {
    graph addIfUnique ManagedExtractedEdge(symbolCompilerId1, relation, symbolCompilerId2)
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