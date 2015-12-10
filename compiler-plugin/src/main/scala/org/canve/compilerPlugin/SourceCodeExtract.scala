package org.canve.compilerPlugin
import tools.nsc.Global
import org.canve.compilerPlugin.Utility._

// TODO: capture leading comment lines, as a separate `comments` property.
//       surely the user will appreciate them, if they can be optionally shown to them.

object AttemptCodeExtract {

  /*
   * Extract the source code of the symbol using compiler supplied ranges 
   *
   * In the obvious case it just mirrors the original indentation in the source code,  
   * which is not included in the range given by the compiler, so it is here "added it back". 
   * 
   * In the less obvious case a definition may not start on a line of its own in the source
   * code (think anonymous definitions). In that case unrelated preceding text will be removed and 
   * replaced by leading spaces rather than only removed. Not optimal for anonymous classes
   * defined with the `new` keyword, but that's a compiler story... 
   */      
  private def grabSymbolCode(global: Global)(symbol: global.Symbol, span: Span) = {
    val sourceFile = symbol.sourceFile.toString
    val sourceFileContents = scala.io.Source.fromFile(sourceFile).mkString
    val firstLineIdentLength = sourceFileContents.slice(0, span.start).reverse.takeWhile(_ != '\n').length   
    
    " " * firstLineIdentLength + sourceFileContents.slice(span.start, span.end)
  }

  def apply(global: Global)(symbol: global.Symbol): Code = {
    
    def logCantDetermine(reason: String) = {
      println(s"Could not determine source definition for symbol ${symbol.nameString} (${symbol.id}) because $reason") 
    }
    
    assert (symbol.sourceFile!=null)
        
    // guard statements necessary given all kinds of special cases
    
    if (symbol.isSynthetic) 
      return Code(symbol.id, CodeLocation(symbol.sourceFile.toString, None), None)                   

    if (symbol.pos.toString == "NoPosition") { 
      // the above can be the case for Scala 2.10 projects, 
      // or just when macros are involved.
      logCantDetermine("pos property is NoPosition") 
      return Code(symbol.id, CodeLocation(symbol.sourceFile.toString, None), None)
    }

    val sourceFilePath = symbol.sourceFile.toString
    val line   = symbol.pos.line
    val column = symbol.pos.column
    val start  = symbol.pos.startOrPoint // plain start may crash for scala 2.10 projects
    val end    = symbol.pos.endOrPoint   // plain end may crash for scala 2.10 projects

    if (line == 0) {
      // the compiler provides a line position 0 sometimes,
      // whereas line numbers are confirmed to start from 1. 
      // Hence we can't extract source here.         
      logCantDetermine("line=0")
      return Code(symbol.id, CodeLocation(sourceFilePath, None), None)
    }
    
    if (start == end) {
      logCantDetermine(s"start=end ($start)")
      println(scala.io.Source.fromFile(sourceFilePath).mkString.slice(start, start+30))
      Code(
        symbol.id, 
        location = CodeLocation(sourceFilePath, Some(Point(start))), 
        code = None)
    }

    else 
      Code(
        symbol.id,
        location = CodeLocation(sourceFilePath, Some(Span(start, end))), 
        code = Some(grabSymbolCode(global)(symbol, Span(start, end))))
  }
}