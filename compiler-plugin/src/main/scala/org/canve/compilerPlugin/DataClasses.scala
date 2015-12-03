package org.canve.compilerPlugin
import tools.nsc.Global

case class ExtractedSymbol 
  (symbolCompilerId: SymbolCompilerId,
   name: String,
   kind: String,
   qualifiedId: QualifiedID,
   
   signatureString: Option[String] = None,  
   
   notSynthetic: Boolean,
   definingProject: DefiningProject) extends SymbolSerialization {
  
     val qualifiedIdAndSignature: FurtherQualifiedID = 
       qualifiedId.pickle + (signatureString match {
         case Some(s) => " * " + s
         case None => "" 
       }) 
     
     def definitionCode(implicit extractedModel: ExtractedModel): Option[Code] = 
       extractedModel.codes.get.get(symbolCompilerId)
     
     /* more inclusive serialization for this class - for logging */
     override def toString = 
       List(symbolCompilerId, 
            name, 
            kind, 
            qualifiedId, 
            signatureString, 
            notSynthetic, 
            definingProject,
            qualifiedIdAndSignature).map(_.toString).mkString(",")
       
     /* symbol and its code info joined into a string - for logging */
     def toJoinedString(implicit extractedModel: ExtractedModel) = toString + ",code: " + definitionCode.toString
          
     var ownersTraversed = false
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
 * types for whether a symbol is defined in 
 * the current project, or is an external one
 */

abstract class DefiningProject
object ProjectDefined    extends DefiningProject
object ExternallyDefined extends DefiningProject

/*
 * a lame join type of a symbol and its extracted code, wherever helpful
 */

case class SymbolCodeJoin(
  extractedModel: ExtractedModel, 
  symbol: ExtractedSymbol) {
  val extractedCode = extractedModel.codes.get.get(symbol.symbolCompilerId)
}