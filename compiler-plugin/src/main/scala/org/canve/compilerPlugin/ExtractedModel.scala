package org.canve.compilerPlugin
import tools.nsc.Global
import performance._
import org.canve.simpleGraph._
import org.canve.simpleGraph.algo.impl.GetPathsBetween
import scala.annotation.tailrec

/*
class IsNoSymbol extends scala.reflect.internal.SymbolTable with scala.reflect.internal.Symbols {
  def apply(s: Symbol): Boolean = 
    s match {
      case NoSymbol => true
      case _ => false
  }
}
*/


/*
 * a class representing a single and complete model extracted for the project being compiled, 
 * comprising symbol details and symbol relations 
 */
class ExtractedModel(global: Global) extends ContainsExtractedGraph {
 
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
            implementation = implementation,
            signatureString = s.signatureString match {
              case("<_>") => None
              case signatureString@_ => Some(signatureString)
            })

        def symString(s: global.Symbol) = s"$s (${s.id})"    
        
        def getSetter(symbol: global.Symbol): Option[global.Symbol] = {
          
          @tailrec
          def impl(symbolOrOwner: global.Symbol): Option[global.Symbol] = {
            (symbolOrOwner.nameString == "<root>") match {
              case true => None
              case false =>
                val maybeSetter = s.setter(symbolOrOwner)
                if (IsSymbol(maybeSetter))
                  Some(maybeSetter)
                else 
                  impl(symbolOrOwner.owner)
            }
          }
          
          impl(s)
        }
        
        def IsSymbol(s: global.Symbol) = s.toString != "<none>" // the scala.reflect.internal class hierarchy doesn't easily succumb to getting a handle to the original NoSymbol class, otherwise we'd use that instead.
        
        //if (s.setter(s).id.toString != "1") println(s"${s.id} has setter: ${s.setter(s).id} (${s.setter})")
        //if (s.getter(s).id.toString != "1") println(s"${symString(s)} has getter: ${s.getter(s).id} (${s.getter})")
        if (s.isSetter) println(s"symbol ${symString(s)} is setter") 
        if (s.isGetter) println(s"symbol ${symString(s)} is getter") 
        if (s.companionSymbol.toString != "<none>") println(s"${symString(s)} has companion: ${symString(s.companionSymbol)}") 
        //if (IsNoSymbol(s))
        if (s.isParameter) println(s"symbol ${symString(s)} is a parameter")
        
        getSetter(s) match {
          case None => println(s"${symString(s)} has no setter")
          case Some(setter) => println(s"${symString(s)} has setter ${symString(setter)}")
        }
        
        graph ++ graph.Vertex(extractedSymbol.symbolCompilerId, extractedSymbol) 
            
        normalization.CompleteOwnerChain(global)(extractedSymbol, s, this)
        
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