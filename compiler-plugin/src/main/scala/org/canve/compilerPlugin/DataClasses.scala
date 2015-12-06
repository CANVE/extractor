package org.canve.compilerPlugin

case class ExtractedSymbolPlus(e: ExtractedSymbol, val implementingProject: String) 
  extends ExtractedSymbol(
    e.symbolCompilerId,
    e.name,
    e.kind,
    e.qualifyingPath,
    e.signatureString,
    e.nonSynthetic,
    e.implementation) {
  
  override def toString = s"${super.toString}, $implementingProject}" 
}

class ExtractedSymbol(
    
  /* 
   * the symbol id drawn from the compiler - only guaranteed to be unique within a single 
   * compilation, meaning a single project's compilation 
  */
  val symbolCompilerId: SymbolCompilerId,
   
  /* symbol's name, or None if the symbol is anonymous */   
  val name: SymbolName, 
   
  /* symbol's kind - package, class, object, method, etc.. */ 
  val kind: String,
   
  /* an identifier similar to Java's FQN, to help uniquely identify a symbol across projects */ 
  val qualifyingPath: QualifyingPath,
   
  /* symbol's type signature, as a string */
  val signatureString: Option[String] = None,   
   
  /* 
   * this is a delicate definition, will likely receive a finer definition going forward. 
   * informally speaking it marks whether the symbol is a contrived one added by the compiler,
   * or just a regular one very directly associated to a source code element. 
   */
  val nonSynthetic: Boolean,

  /* 
   * says whether the symbol's implementation is defined in the current project, or externally to it.
   * for example a symbol may be one that has its implementation coming from a library, or from a depended-upon 
   * subproject summoned into the compilation classpath by sbt or other tool. In those cases, this marks that
   * the symbol's implementation resides externally, not within the project being compiled.  
   */
  val implementation: ImplementationLoc) extends SymbolSerialization { 
  
    /* getter for symbol's code description, the latter kept in a separate collection */
    def definitionCode(implicit extractedModel: ExtractedModel): Option[Code] = 
      extractedModel.codes.get.get(symbolCompilerId)
     
    /* more inclusive serialization for this class - for logging */
    override def toString = 
      List(symbolCompilerId, 
           name, 
           kind, 
           qualifyingPath, 
           signatureString, 
           nonSynthetic, 
           implementation).map(_.toString).mkString(",")
       
    /* symbol and its code info joined into a string - for logging */
    def toJoinedString(implicit extractedModel: ExtractedModel) = toString + ",code: " + definitionCode.toString
          
    var ownersTraversed = false // TODO: not needed by those that inherit, so further refactor to base class that excludes this var 
}

object ExtractedSymbol {
  
  /* 
   * just the obvious apply, to provide a case class instantiation style 
   */
  def apply(
    symbolCompilerId: SymbolCompilerId,
    name: SymbolName, 
    kind: String,
    qualifyingPath: QualifyingPath,
    signatureString: Option[String],   
    nonSynthetic: Boolean,
    implementation: ImplementationLoc) = 
      new ExtractedSymbol(
        symbolCompilerId,
        name,
        kind,
        qualifyingPath,
        signatureString,
        nonSynthetic,
        implementation)
}

/*
 * symbol's extracted source code location and content
 */

case class Code
  (symbolCompilerId: Int,
   location: CodeLocation, // the location of the symbol's definition
   code: Option[String])   // the code extracted, if any

case class CodeLocation(
  path: String,              // the source (file) path  
  position: Option[Position] // the location within that source
) 
   
/* differentiate two types of location provided by the compiler */
abstract class Position
case class Span(start: Int, end: Int) extends Position
case class Point(init: Int) extends Position { def apply = init }
   
/*
 * types for whether a symbol is defined in 
 * the current project, or is an external one
 * being referenced by this project
 */
abstract class ImplementationLoc
object ProjectDefined    extends ImplementationLoc
object ExternallyDefined extends ImplementationLoc

/*
 * Holds a fully qualified identifier to a symbol, that should 
 * uniquely identify a symbol across projects
 */
case class FQI(qPath: QualifyingPath, signatureString: Option[String])

object FQI {
  def apply(s: ExtractedSymbol) = new FQI(s.qualifyingPath, s.signatureString) 
}

/*
 * a lame join type of a symbol and its extracted code, if helpful
 */
case class SymbolCodeJoin(
  extractedModel: ExtractedModel, 
  symbol: ExtractedSymbol) {
  val extractedCode = extractedModel.codes.get.get(symbol.symbolCompilerId)
}