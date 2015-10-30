package Logging
import tools.nsc.Global

object Utility {
  
  def logEdge(nodeA: Int, relation: String, nodeB: Int) = {    
    println(nodeA + " " + relation + " " + nodeB)
  }
  
  def logSymbol(global: Global)(symbol: global.Symbol) = {
    println(symbolWithId(global)(symbol))
  }
  
  def symbolWithId(global: Global)(symbol: global.Symbol) = {
    symbol.nameString + " (" + symbol.id + ") "
  }

  object Warning {
  
    def logMemberParentLacking(global: Global)(symbol: global.Symbol) = {    
      println(Console.MAGENTA + Console.BOLD + symbol.kindString + Console.RESET)
      //println(Console.MAGENTA + Console.BOLD + showRaw(tree) + Console.RESET)
    }
      
    def logParentNotOwner(global: Global)(parent: global.Symbol, owner: global.Symbol) = {
      println(Console.YELLOW + "parent not owner! " + 
                               symbolWithId(global)(parent) + ", " + 
                               symbolWithId(global)(owner) +
                               Console.RESET)
    }  
  }
}

