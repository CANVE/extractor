package org.canve.compilerPlugin
import tools.nsc.Global
import performance._
import org.canve.simpleGraph._
import org.canve.simpleGraph.algo.impl.GetPathsBetween
import play.api.libs.json._

/*
 * a class representing a single and complete model extracted for the project being compiled, 
 * comprising symbol details and symbol relations 
 */
class ExtractedModel(global: Global) extends ContainsExtractedGraph with DataLogger {
 
  val TraversalSymbolRevisit = Counter("TraversalSymbolRevisit")
  
  val codes = new ExtractedCodes
  
  def addOrGet(global: Global)(s: global.Symbol): ExtractedSymbol = {
    graph.vertex(s.id) match {
      case Some(v: graph.Vertex) => v.data 
      case None => add(global)(s)
    }
  }
  
  def add(global: Global)(s: global.Symbol): ExtractedSymbol = {
    graph.vertex(s.id) match {
      
      case Some(v: graph.Vertex) =>
        //throw ExtractionException(s"graph already has symbol with id ${s.id}")
        TraversalSymbolRevisit.increment
        v.data
      
      case None =>
        
        val name = getUniqueName(global)(s)
        
        val qualifyingPath = QualifyingPath(global)(s)
  
        /*
         * determine whether the symbol at hand is defined in the current project,
         * see https://github.com/CANVE/extractor/issues/8 
         */ 
        val implementation = s.sourceFile match { 
          case null => ExternallyDefined // no source file for this entity = external symbol
          case _    => ProjectDefined
        }

        /*
         * attempt extracting the symbol's source code, 
         * if it is defined in the current project 
         */
        val code: Option[Code] = implementation match {
          case ProjectDefined    => 
            val code = AttemptCodeExtract(global)(s)
            codes.maybeAdd(global)(s, code)
            Some(code)
          case ExternallyDefined => None
        }
        
        val isParameter = s.isGetter 
        
        val isTypeParameter = s.isTypeParameter

        /*
         * add the symbol to the extracted model
         */
        val extractedSymbol = 
          ExtractedSymbol(
            symbolCompilerId = s.id, 
            name = name,
            kind = s.kindString, 
            codeLocation = code.map(_.location), 
            qualifyingPath = qualifyingPath, 
            nonSynthetic = !(s.isSynthetic), 
            isParameterAccessor = s.isParamAccessor,
            isParameter = s.isParameter,
            isTypeParameter = s.isTypeParameter,
            isSetter = s.isSetter,
            isGetter = s.isGetter,
            implementation = implementation,
            signatureString = s.signatureString match {
              case("<_>") => None
              case signatureString@_ => Some(signatureString)
            })
                
        //if (IsNoSymbol(s))
        
        //if (s.setter(s).id.toString != "1") println(s"${s.id} has setter: ${s.setter(s).id} (${s.setter})")
        //if (s.getter(s).id.toString != "1") println(s"${symString(s)} has getter: ${s.getter(s).id} (${s.getter})")
        
        graph ++ graph.Vertex(extractedSymbol)
        
        ExtraTraversalRelations(global)(s)(this)
        
        normalization.CompleteOwnerChain(global)(extractedSymbol, s, this)
        
        TypeExtractionSpike.getType(global)(s)(this)
        
        extractedSymbol
    }
  }
  
  def add(symbolCompilerId1: SymbolCompilerId, edgeKind: ExtractedSymbolRelation, symbolCompilerId2: SymbolCompilerId) = {
    graph addIfUnique graph.Edge(symbolCompilerId1, symbolCompilerId2, edgeKind)
  }
  
  def addIfUnique(symbolCompilerId1: SymbolCompilerId, relation: ExtractedSymbolRelation, symbolCompilerId2: SymbolCompilerId) = {
    graph addIfUnique graph.Edge(symbolCompilerId1, symbolCompilerId2, relation)
  }
}

class ExtractedCodes {
  
  var map: Map[Int, Code] = Map()
  
  def maybeAdd(global: Global)(s: global.Symbol, code: Code) = {
    map.contains(s.id) match {
      case false => map += (s.id -> code)
      case true  => // do nothing here 
    }
  }
  
  def get = map
}

/*
class IsNoSymbol extends scala.reflect.internal.SymbolTable with scala.reflect.internal.Symbols {
  def apply(s: Symbol): Boolean = 
    s match {
      case NoSymbol => true
      case _ => false
  }
}
*/

object ExtraSymbolProperties extends DataLogger {
  def apply(global: Global)(s: global.Symbol) {
    def symString(s: global.Symbol) = s"$s (${s.id})"
    var detectedProperties = 0
    if (s.isSetter) { dataLogSpecialProperty(symString(s), "setter"); detectedProperties+=1 } 
    if (s.isGetter) { dataLogSpecialProperty(symString(s), "getter"); detectedProperties+=1 }
    if (s.isParameter) { dataLogSpecialProperty(symString(s), "a parameter"); detectedProperties+=1 }
    if (s.isTypeParameter) { dataLogSpecialProperty(symString(s), "a type parameter"); detectedProperties+=1 }
    if (s.isParamAccessor) { dataLogSpecialProperty(symString(s), "a parameter accessor"); detectedProperties+=1 }

    if (detectedProperties > 1) 
      println(s"symbol ${symString(s)} sharing more than one special property (${s.isSetter} ${s.isGetter} ${s.isParameter} ${s.isTypeParameter})")
    if (s.isTypeParameter && !s.isParameter)
      println(s"symbol ${symString(s)} is marked by the compiler as a type parameter and not also as a parameter")      
    if (s.isSetter && s.isGetter)
      println("symbol ${symString(s)} is marked by the compiler as both setter and getter")
    //def printIfTrue(x: T): Unit = macro       
  }
}