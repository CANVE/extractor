package org.canve.compilerPlugin

/*
 * helper for writing node data to csv 
 */
trait SymbolSerialization {
  self: ExtractedSymbol =>
  
  def toCsvRow: String = {
    List(
      definingProject match {
        case ProjectDefined    => "project"
        case ExternallyDefined => "external" },
      notSynthetic,
      symbolCompilerId, 
      name, 
      kind,
      qualifiedId.pickle
    ).mkString(",")
  }
}

/*
 * Symbol constructor from CANVE CSV data row
 */
object SymbolFromCsvRow {
  def apply(projectName: String, rowMap: Map[String, String]): ExtractedSymbol = { 
     ExtractedSymbol(symbolCompilerId = rowMap("id").toInt,
          name = rowMap("name"),
          kind = rowMap("kind"),
          notSynthetic = rowMap("notSynthetic").toBoolean,
          qualifiedId = QualifiedID.unpickle(rowMap("qualifiedId")),
          definingProject = rowMap("definition") match {
            case "project" => ProjectDefined
            case "external" => ExternallyDefined
          })
  }
}

/*
/*
 * Symbol relation from CANVE CSV data row
 */
object SymbolRelationFromCsvRow {
  def apply(rowMap: Map[String, String]) = 
    ExtractedSymbolRelation(
      rowMap("id1").toInt,
      rowMap("id2").toInt,
      rowMap("relation"))
}
*/