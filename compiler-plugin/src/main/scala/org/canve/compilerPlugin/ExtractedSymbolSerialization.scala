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
      quoteWrap(name), 
      kind,
      symbolCompilerId, 
      nonSynthetic,
      isParameterAccessor,
      isParameter,
      isTypeParameter,
      isSetter,
      isGetter,
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
      implementation = rowMap("implementation") match {
        case "project" => ProjectDefined
        case "external" => ExternallyDefined
      },        
      name = SymbolName(rowMap("name")), 
      kind = rowMap("kind"),
      symbolCompilerId = rowMap("id").toInt,
      nonSynthetic = rowMap("nonSynthetic").toBoolean,
      isParameterAccessor = rowMap("isParameterAccessor").toBoolean,
      isParameter = rowMap("isParameter").toBoolean,
      isTypeParameter = rowMap("isTypeParameter").toBoolean,
      isSetter = rowMap("isSetter").toBoolean,
      isGetter = rowMap("isGetter").toBoolean,
      codeLocation = toStringOption(rowMap("codeLocation")).map(toClassArgs).map(s => CodeLocation(s)),
      qualifyingPath = QualifyingPath(rowMap("qualifyingPath")),
      signatureString = toStringOption(rowMap("signature"))
      )
  }
}