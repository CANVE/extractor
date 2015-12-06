package org.canve.compilerPlugin

/*
 * Serialize extracted symbol into CSV data row
 */
trait ExtractedSymbolSerialization {
  self: ExtractedSymbol =>
  
  def toCsvRow(implicit extractedModel: ExtractedModel): String = {
    // Note: escaping for csv is for now handled here by hand (likely a tad faster).
    List(
      implementation match {
        case ProjectDefined    => "project"
        case ExternallyDefined => "external" },
      nonSynthetic,
      symbolCompilerId, 
      name, 
      kind,
      "\"" + codeLocation + "\"",
      qualifyingPath,
      "\"" + signatureString + "\"" // escaped as it contains, typically, commas
    ).mkString(",")
  }
}

/*
 * Symbol constructor from CANVE CSV data row
 */
trait ExtractedSymbolDeserialization extends SerializationUtil {
  def apply(projectName: String, rowMap: Map[String, String]): ExtractedSymbol = { 
    ExtractedSymbol(
      symbolCompilerId = rowMap("id").toInt,
      name = SymbolName(rowMap("name")), 
      kind = rowMap("kind"),
      codeLocation = toStringOption(rowMap("codeLocation")).map(toClassArgs).map(s => CodeLocation(s)),
      nonSynthetic = rowMap("nonSynthetic").toBoolean,
      qualifyingPath = QualifyingPath(rowMap("qualifyingPath")),
      signatureString = toStringOption(rowMap("signature")),
      implementation = rowMap("implementation") match {
        case "project" => ProjectDefined
        case "external" => ExternallyDefined
      })
  }
}

trait SerializationUtil {
  def toStringOption(s: String): Option[String] = {
    s match {
      case "None" => None
      case _ => Some(s.drop("Some(".length).dropRight(1))
    }
  }
  
  def toClassArgs(s: String): String = {
    s.dropWhile(_ != '(').drop(1).dropRight(1)
  }
}