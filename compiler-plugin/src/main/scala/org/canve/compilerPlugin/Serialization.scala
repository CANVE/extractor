package org.canve.compilerPlugin

/*
 * Serialize extracted symbol into CSV data row
 */
trait SymbolSerialization {
  self: ExtractedSymbol =>
  
  def toCsvRow(implicit extractedModel: ExtractedModel): String = {
    // Note: escaping for csv is for now handled here by hand (likely a tad faster)
    List(
      implementation match {
        case ProjectDefined    => "project"
        case ExternallyDefined => "external" },
      nonSynthetic,
      symbolCompilerId, 
      name, // TODO: serialize harmoniously to the reader below and code that uses it
      kind,
      qualifyingPath.pickle,
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
          name = new SymbolName(rowMap("name")), // TODO: this is not the correct way to deserialize the field as outputted
          kind = rowMap("kind"),
          nonSynthetic = rowMap("nonSynthetic").toBoolean,
          qualifyingPath = QualifyingPath(rowMap("qualifiedId")),
          signatureString = deSerializeOption(rowMap("signature")),
          implementation = rowMap("implementation") match {
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