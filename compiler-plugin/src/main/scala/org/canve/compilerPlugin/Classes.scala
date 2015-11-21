package org.canve.compilerPlugin
import tools.nsc.Global

case class ExtractedSymbolRelation
  (symbolID1: Int,
   relation: String,
   symbolID2: Int) 

/*
 * types for whether a symbol is defined in 
 * the current project, or is an external one
 */
   
abstract class DefiningProject
object ProjectDefined    extends DefiningProject
object ExternallyDefined extends DefiningProject

case class ExtractedSymbol 
  (id: Int,
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

abstract class Location
object NoLocationInfo extends Location
case class Span(start: Int, end: Int) extends Location
case class Point(init: Option[Int] = None) extends Location { def apply = init }

case class ExtractedCode
  (sourcePath: String,   // the source path code was extracted from
   location: Location,   // the location within that source
   code: Option[String]) // the code extracted
   
   