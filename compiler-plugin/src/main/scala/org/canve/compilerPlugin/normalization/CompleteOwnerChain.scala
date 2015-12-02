package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
import scala.tools.nsc.Global

/*
 * Assure that a symbol's hierarchy chain has been captured -  
 * this is needed for the case that the node is a library symbol, 
 * so we won't (necessarily) bump into its parents while compiling
 * the project being compiled. And also for ultimately merging symbols
 * from different projects
 */

object CompleteOwnerChain {
  def apply
    (global: Global)
    (extractedSymbol: ExtractedSymbol, s: global.Symbol, model: ExtractedModel): Unit = {
    import global._ // for access to typed symbol methods
    
    def impl(node: ExtractedSymbol, s: global.Symbol): Unit = {
      if (!extractedSymbol.ownersTraversed) {
        if (s.nameString != "<root>") {
          val ownerSymbol = s.owner
          val ownerNode = model.addOrGet(global)(ownerSymbol)
          model.addIfUnique(s.owner.id, "declares member", s.id)
          impl(ownerNode, ownerSymbol)
          
          extractedSymbol.ownersTraversed = true 
        }
      }
    }
    
    impl(extractedSymbol, s)
  }
}