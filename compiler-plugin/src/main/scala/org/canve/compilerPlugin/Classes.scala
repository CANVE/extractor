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
   definingProject: DefiningProject)
   extends SymbolSerialization {
     var ownersTraversed = false 
}

/*
 * symbol's extracted source code
 */

abstract class MaybePosition
object NoPosition extends MaybePosition // TODO: this no longer belongs in here maybe
case class Span(start: Int, end: Int) extends MaybePosition
case class Point(init: Int) extends MaybePosition { def apply = init }

case class SourceCodeLocation(
  path: String,           // the source (file) path  
  position: MaybePosition // the location within that source
) 

case class ExtractedCode
  (symbolCompilerId: Int,
   codeLocation: SourceCodeLocation,
   code: Option[String]) // the code extracted
   
   