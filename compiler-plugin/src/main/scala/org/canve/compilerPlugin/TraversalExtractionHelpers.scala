package org.canve.compilerPlugin
import tools.nsc.Global
import play.api.libs.json._
import scala.annotation.tailrec
import scala.reflect.internal.Types

/*
 * Capturing relations which are expressed / extracted by symbol properties rather than
 * by the order of traversal. 
 */
object ExtraTraversalRelations extends DataLogger {
  
  def apply(global: Global)(s: global.Symbol)(extractedModel: ExtractedModel) {
    
    if (s.companionSymbol.toString != "<none>") {
      extractedModel.add(global)(s.companionSymbol) // maybe this is never needed with the current flow
      extractedModel.add(s.id, "has companion", s.companionSymbol.id)
      //DataLogExtraRelation(symString(s), "has companion", symString(s.companionSymbol)) 
    }        
    
    getSetter(global)(s) match {
      case None => // println(s"${symString(s)} has no setter")
      case Some(setter) =>
        //DataLogExtraRelation(symString(s), "has setter", symString(setter))
        extractedModel.add(global)(setter) // maybe this is never needed with the current flow
        extractedModel.add(setter.id, "is setter for" , s.id)
    }       
    
    getGetter(global)(s) match {
      case None => // println(s"${symString(s)} has no setter")
      case Some(getter) =>
        //DataLogExtraRelation(symString(s), "has setter", symString(setter))
        extractedModel.add(global)(getter) // maybe this is never needed with the current flow
        extractedModel.add(getter.id, "is getter for" , s.id)
    }       
        
    
    /*
     * Record overriding relationships for symbols this symbol overrides 
     */
    s.allOverriddenSymbols.foreach { overriden => 
      extractedModel.add(global)(overriden)
      extractedModel.add(s.id, "overrides", overriden.id)
    }    
  }
  
  /* 
   * Identifies a variable's synthetic setter symbol if it has one
   * the recursive algorithm was found through some data research,
   * admittedly does not seem very intuitive.
   */
  def getSetter(global: Global)(s: global.Symbol): Option[global.Symbol] = {
   
    // the scala.reflect.internal class hierarchy doesn't easily succumb to getting a handle 
    // to the original NoSymbol class, otherwise we'd use that instead.          
    def IsSymbol(s: global.Symbol) = s.toString != "<none>" 
    
    @tailrec 
    def impl(symbolOrOwner: global.Symbol): Option[global.Symbol] = {
      (symbolOrOwner.nameString == "<root>") match { // might possibly use .isRoot* instead..
        case true => None
        case false =>
          val setter = s.setter(symbolOrOwner)
          if (IsSymbol(setter) && setter != s)
            setter.isSetter match {
              case true  => Some(setter)
              case false => None
          }
          else 
            impl(symbolOrOwner.owner)
      }
    }
    
    impl(s)
  }

  /* 
   * Experimental clone of the getSetter algorithm
   */
  def getGetter(global: Global)(s: global.Symbol): Option[global.Symbol] = {
   
    // the scala.reflect.internal class hierarchy doesn't easily succumb to getting a handle 
    // to the original NoSymbol class, otherwise we'd use that instead.          
    def IsSymbol(s: global.Symbol) = s.toString != "<none>" 
    
    @tailrec 
    def impl(symbolOrOwner: global.Symbol): Option[global.Symbol] = {
      (symbolOrOwner.nameString == "<root>") match { // might possibly use .isRoot* instead..
        case true => None
        case false =>
          val getter = s.getter(symbolOrOwner)
          if (IsSymbol(getter) && getter != s)
            getter.isGetter match {
              case true  => Some(getter)
              case false => None
          }
          else 
            impl(symbolOrOwner.owner)
      }
    }
    
    impl(s)
  }  
  
}
  
trait DataLogger { 
  def dataLogSpecialProperty(key: String, value: String) {
    MetaDataLog(key, Json.parse(s""" { "is" : "$value" } """))
  }
  def DataLogExtraRelation(symbol1: String, specialRelation: String, symbol2: String) {
    MetaDataLog(s"$symbol1&$symbol2" , 
            Json.parse(s""" 
              { "$specialRelation": {
                   "symbol1" : "$symbol1",
                   "symbol2" : "$symbol2"
                 }
              } 
            """)
           )
  }
}