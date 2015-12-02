package org.canve.compilerPlugin
import tools.nsc.Global

/*
 * types for whether a symbol is defined in 
 * the current project, or is an external one
 */
   
abstract class DefiningProject
object ProjectDefined    extends DefiningProject
object ExternallyDefined extends DefiningProject

case class ExtractedSymbol 
  (symbolCompilerId: SymbolCompilerId,
   name: String,
   kind: String,
   notSynthetic: Boolean,
   qualifiedId: QualifiedID,
   definingProject: DefiningProject) extends SymbolSerialization {
  
     var ownersTraversed = false
     
     def definitionCode(implicit extractedModel: ExtractedModel): Option[Code] = 
       extractedModel.codes.get.get(symbolCompilerId)
       
     def toJoinedString(implicit extractedModel: ExtractedModel) = this.toString + definitionCode.toString  
}

/*
 * symbol's extracted source code location and content
 */

case class Code
  (symbolCompilerId: Int,
   location: CodeLocation, // the location of the symbol's definition
   code: Option[String])   // the code extracted, if any

case class CodeLocation(
  path: String,              // the source (file) path  
  position: Option[Position] // the location within that source
) 
   
/* differentiate two types of location provided by the compiler */
abstract class Position
case class Span(start: Int, end: Int) extends Position
case class Point(init: Int) extends Position { def apply = init }
   
/*
 * Simply a join of a symbol and its extracted code, wherever helpful
 */
case class SymbolCodeJoin(
  extractedModel: ExtractedModel, 
  symbol: ExtractedSymbol) {
  val extractedCode = extractedModel.codes.get.get(symbol.symbolCompilerId)
}