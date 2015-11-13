package org.canve.compilerPlugin

/*
 * helper for writing node data to csv 
 */
trait NodeSerialization {
  self: ExtractedSymbol =>
  
  def toCsvRow: String = {
    List(definingProject match {
          case ProjectDefined    => "project"
          case ExternallyDefined => "external" },
         notSynthetic,
         id, 
         name, 
         kind,
         qualifiedId).mkString(",")
  }
}

/*
 * Symbol constructor from CANVE CSV data row
 */
object SymbolFromCsvRow {
  def apply(projectName: String, rowMap: Map[String, String]): ExtractedSymbol = { 
     ExtractedSymbol(id = rowMap("id").toInt,
          name = rowMap("name"),
          kind = rowMap("kind"),
          notSynthetic = rowMap("notSynthetic").toBoolean,
          qualifiedId = rowMap("qualifiedId"),
          definingProject = rowMap("definition") match {
            case "project" => ProjectDefined
            case "external" => ExternallyDefined
          },
          definingFileName = None,
          sourceCode = None)
  }
}

/*
 * Symbol relation from CANVE CSV data row
 */
object SymbolRelationFromCsvRow {
  def apply(rowMap: Map[String, String]): ExtractedSymbolRelation = {
    ExtractedSymbolRelation(symbolID1 = rowMap("id1").toInt,
         relation = rowMap("relation"),
         symbolID2 = rowMap("id2").toInt)
  }
}