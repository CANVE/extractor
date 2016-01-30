package org.canve.compilerPlugin
import scala.tools.nsc.Global

object TypeExtraction {

 private def logType(s: String) {
   println("[type] " + s)
 }
  
 def getType(global: Global)(symbol: global.Symbol)(extractedModel: ExtractedModel): global.Symbol = {
   import global._
   val typeSymbol = symbol.tpe.typeSymbol

   logType(s"$symbol is of type $typeSymbol")
   logType(s"$symbol has type bounds ${getTypeBounds(global)(symbol)(extractedModel)}")
   
   logType(s"$symbol has type arguments: ") 
   getTypeTypeArgs(global)(symbol.tpe)(extractedModel)
   
   getSymbolTypeParams(global)(symbol)(extractedModel).foreach { s =>
     logType(s"$symbol has type parameter ${s.nameString} ")
     getType(global)(s)(extractedModel)
   }
   
   /* 
    * e.g. when a function def has a generic, but in that case seems just redundant 
    * Will keep comparing it to what comes from getSymbolTypeParams, in order to 
    * detect cases where it is not superfluous, then infer what they mean. 
    */
   getTypeTypeParams(global)(symbol.tpe)(extractedModel).foreach { s =>
     logType(s"$symbol's type ${symbol.tpe} has type parameter (from symbol's type) ${s.nameString}")
     getType(global)(s)(extractedModel)
   }
   
   typeSymbol
  }

  def getTypeTypeArgs(global: Global)(ttype: global.Type)(extractedModel: ExtractedModel): Unit = {
    import global._
    ttype.typeArgs.foreach { tparam =>
      logType(s"$ttype has type argument ${tparam.typeSymbol.nameString}")
      getTypeTypeArgs(global)(tparam)(extractedModel)
    }
  }  
  
  def getSymbolTypeParams(global: Global)(symbol: global.Symbol)(extractedModel: ExtractedModel): List[global.Symbol] = {
    import global._
    symbol.typeParams
  }    
 
  def getTypeTypeParams(global: Global)(ttype: global.Type)(extractedModel: ExtractedModel): List[global.Symbol] = {
    import global._
    ttype.typeParams
  }     
  
  def getTypeBounds(global: Global)(typeSymbol: global.Symbol)(extractedModel: ExtractedModel): (global.Type, global.Type) = {
    import global._
    val TypeBounds(lower, higher) = typeSymbol.tpe.bounds
      //if (higher.toString != "Nothing")
        //extractedModel.add(global)(higher.typeSymbol)
        //extractedModel.addIfUnique(typeSymbol.id, "has higher type bound", higher.typeSymbol.id)
      //if (lower.toString != "Nothing")
        //println("foo")
        //extractedModel.add(global)(lower.typeSymbol)
        //extractedModel.addIfUnique(typeSymbol.id, "has lower type bound", lower.typeSymbol.id)
          
    (lower, higher)
  }
 
}