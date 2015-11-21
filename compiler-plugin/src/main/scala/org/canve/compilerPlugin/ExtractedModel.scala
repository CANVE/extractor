package org.canve.compilerPlugin
import tools.nsc.Global
import performance.Counters
import org.canve.simpleGraph.{AbstractVertex, AbstractEdge}

/*
 * a class representing a single and complete model extracted for the project being compiled, 
 * comprising symbol details and symbol relations 
 */
class ExtractedModel {
  
  /*
   * Captures the node's hierarchy chain -  
   * this is needed for the case that the node is a library symbol, 
   * so we won't (necessarily) bump into its parents while compiling
   * the project being compiled. And also for ultimately merging symbols
   * from different projects
   */
  private def assureOwnerChain(global: Global)(node: ExtractedSymbol, symbol: global.Symbol): Unit = {
    import global._ // for access to typed symbol methods
    
    def impl(node: ExtractedSymbol, s: global.Symbol): Unit = {
      // Note: there is also the reflection library supplied Node.ownerChain method,
      //       for directly getting the chain of owners. So this could be rewritten
      //       as a loop going over the result of that library supplied function...
      //       or juxtuposed 
      if (!node.ownersTraversed) {
        if (s.nameString != "<root>") {
          val ownerSymbol = s.owner
          val ownerNode = symbols(global)(ownerSymbol)
          symbolRelations(s.owner.id, "declares member", s.id)
          impl(ownerNode, ownerSymbol)
          node.ownersTraversed = true 
        }
      }
    }
    
    impl(node, symbol)
  }
  
  val symbols = new ExtractedSymbols
  val symbolRelations = new ExtractedSymbolRelations
  val codes = new ExtractedCodes
  
  def add(global: Global)(s: global.Symbol): Unit = {
    if (!symbols.has(s.id)){

      val extractedSymbol: ExtractedSymbol = symbols(global)(s)
      
      /*
       * extract the symbol's source code, if possible 
       */
      
      s.sourceFile match {
        case null => // no source file included in this project for this entity
        case _    => codes(global)(s, CodeExtract(global)(s))
      }
      
      assureOwnerChain(global)(extractedSymbol, s)  
    }
  }
  
  def add(id1: Int, edgeKind: String, id2: Int) = symbolRelations.apply(id1, edgeKind, id2)
}

/*
 * an extracted symbols collection (singleton as we run once per project)
 */
class ExtractedSymbols {
  
  val existingCalls = Counters("existing node calls")
  
  private var map: Map[Int, ExtractedSymbol] = Map()
  
  def apply(global: Global)(s: global.Symbol): ExtractedSymbol = {
    
    map.contains(s.id) match {
      
      case true =>
        existingCalls.increment
        map.get(s.id).get      
        
      case false =>
        val kindNameList: List[KindAndName] = s.ownerChain.reverse.map(owner => KindAndName(owner.kindString, owner.nameString))
        assert(kindNameList.head.kind == "package")
        assert(kindNameList.head.name == "<root>")
        val qualifiedId = QualifiedID(kindNameList.drop(1))
        
        /*
         * determine whether the symbol at hand is defined in the current project, 
         */ 
        val definingProject = s.sourceFile match {
          case null => ExternallyDefined // no source file included in this project for this entity
          case _    => ProjectDefined
        }
        
        val newNode = ExtractedSymbol(s.id, s.nameString, s.kindString, !(s.isSynthetic), qualifiedId, definingProject)
        
        map += (s.id -> newNode)
        newNode
    }
  }
  
  def has(id: Int) = map.contains(id)
  
  def size = map.size

  def get = map.map(_._2)
  
}

/*
 * an objects collection (singleton as we run once per project)
 */
class ExtractedSymbolRelations {
  
  val existingCalls = Counters("existing edge calls")
  
  private var set: Set[ExtractedSymbolRelation] = Set()
  
  def apply(id1: Int, edgeKind: String, id2: Int) {
    
    val edge = ExtractedSymbolRelation(id1, edgeKind, id2)
    if (set.contains(edge)) 
      existingCalls.increment
    else
      set = set + edge
  }
  
  def get = set
  
  def size = set.size
}

class ExtractedCodes {
  
  private var map: Map[Int, ExtractedCode] = Map()
  
  def apply(global: Global)(s: global.Symbol, code: ExtractedCode) = {
    map.contains(s.id) match {
      case false => map += (s.id -> code)
      case true  => // do nothing 
    }
  }
  
  def get = map
}