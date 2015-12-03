package org.canve.compilerPlugin

/*
 * Serialize extracted symbol into CSV data row
 */
trait SymbolSerialization {
  self: ExtractedSymbol =>
  
  def toCsvRow(implicit extractedModel: ExtractedModel): String = {
    // Note: escaping for csv is for now handled here by hand (likely a tad faster)
    List(
      definingProject match {
        case ProjectDefined    => "project"
        case ExternallyDefined => "external" },
      notSynthetic,
      symbolCompilerId, 
      name, 
      kind,
      qualifiedId.pickle,
      "\"" + signatureString + "\"" // escaped as it contains, typically, commas
    ).mkString(",")
  }
}

/*
 * Symbol constructor from CANVE CSV data row
 */
object SymbolFromCsvRow {
  import Util._
  def apply(projectName: String, rowMap: Map[String, String]): ExtractedSymbol = { 
     ExtractedSymbol(symbolCompilerId = rowMap("id").toInt,
          name = rowMap("name"),
          kind = rowMap("kind"),
          notSynthetic = rowMap("notSynthetic").toBoolean,
          qualifiedId = QualifiedID.unpickle(rowMap("qualifiedId")),
          signatureString = deSerializeOption(rowMap("signature")),
          definingProject = rowMap("definition") match {
            case "project" => ProjectDefined
            case "external" => ExternallyDefined
          })
  }
}

object Util {
  def deSerializeOption[T](s: String): Option[T] = {
    s match {
      case "None" => None
      case s@_ => Some(s.drop("Some(".length).dropRight(1).asInstanceOf[T])
    }
  }
}