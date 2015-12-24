package org.canve.compilerPlugin
import org.canve.util._

/*
 * Serialize extracted symbol into CSV data row
 */
trait ExtractedSymbolSerialization {
  self: ExtractedSymbol =>
  
  def escapeQuotes(s: String): String = {
    s.replace("\"", "\"\"")
  }
  
  def quoteWrap(a: Any) = "\"" + a + "\"" 
    
  def toCsvRow(implicit extractedModel: ExtractedModel): String = {
    // Note: escaping for csv is for now handled by hand (likely a tad faster).
    List(
      implementation match {
        case ProjectDefined    => "project"
        case ExternallyDefined => "external" },
      nonSynthetic,
      symbolCompilerId, 
      quoteWrap(name), 
      kind,
      quoteWrap(codeLocation),
      quoteWrap(qualifyingPath),
      quoteWrap(escapeQuotes(signatureString.toString)) 
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