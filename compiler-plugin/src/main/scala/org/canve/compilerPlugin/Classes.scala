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
  definingProject: DefiningProject,
  definingFileName: Option[String],
  sourceCode: Option[String],
  merged: Boolean = false) 
  extends SymbolSerialization {
    var ownersTraversed = false 
}