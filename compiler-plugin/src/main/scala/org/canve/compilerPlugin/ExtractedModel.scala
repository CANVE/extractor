package org.canve.compilerPlugin
import tools.nsc.Global
import performance.Counters
import org.canve.simpleGraph.{AbstractVertex, AbstractEdge}

/*
 * a class representing a single and complete model extracted for the project being compiled, 
 * comprising symbol details and symbol relations 
 */
class ExtractedModel(global: Global) {
  
  /*
   * Captures the node's hierarchy chain -  
   * this is needed for the case that the node is a library symbol, 
   * so we won't (necessarily) bump into its parents while compiling
   * the project being compiled. And also for ultimately merging symbols
   * from different projects
   */
  private def assureOwnerChain(global: Global)(managedExtractedSymbol: ManagedExtractedSymbol, s: global.Symbol): Unit = {
    import global._ // for access to typed symbol methods
    
    def impl(node: ManagedExtractedSymbol, s: global.Symbol): Unit = {
      if (!managedExtractedSymbol.data.ownersTraversed) {
        if (s.nameString != "<root>") {
          val ownerSymbol = s.owner
          val ownerNode = addOrGet(global)(ownerSymbol)
          add(s.owner.id, "declares member", s.id)
          impl(ownerNode, ownerSymbol)
          managedExtractedSymbol.data.ownersTraversed = true 
        }
      }
    }
    
    impl(managedExtractedSymbol, s)
  }
  
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
      case Some(__) => throw ExtractionException(s"graph already has symbol with id ${s.id}")
      case None =>
        val qualifiedId = QualifiedID.compute(global)(s)
  
        /*
         * determine whether the symbol at hand is defined in the current project, 
         */ 
        val definingProject = s.sourceFile match {
          case null => ExternallyDefined // no source file included in this project for this entity
          case _    => ProjectDefined
        }
  
        val extractedSymbol = ExtractedSymbol(s.id, s.nameString, s.kindString, !(s.isSynthetic), qualifiedId, definingProject)
        val managedExtractedSymbol = ManagedExtractedSymbol(extractedSymbol) 
        graph += managedExtractedSymbol
              
        /*
         * extract the symbol's source code, if possible 
         */
        
        s.sourceFile match {
          case null => // no source file included in this project for this entity
          case _    => codes(global)(s, CodeExtract(global)(s))
        }
        
        assureOwnerChain(global)(managedExtractedSymbol, s)
        managedExtractedSymbol
    }
  }
  
  def add(symbolCompilerId1: SymbolCompilerId, edgeKind: ExtractedSymbolRelation, symbolCompilerId2: SymbolCompilerId) = {
    graph += ManagedExtractedEdge(symbolCompilerId1,edgeKind,symbolCompilerId2)
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