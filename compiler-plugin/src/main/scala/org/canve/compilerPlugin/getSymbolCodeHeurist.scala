package org.canve.compilerPlugin
import tools.nsc.Global
import org.canve.compilerPlugin.Utility._

/*
 * heuristic based extraction - may be necessary for supporting scala 2.10 so keep it alive in compilation
 * (c.f. https://github.com/scoverage/scalac-scoverage-plugin/blob/5d0c92479dff0055f2cf7164439f838b803fe44a/2.10.md)
 * 
 * This heuristic algorithm is not needed, as long as -Yrangepos is used, but since -Yrangepos 
 * may crash on scala 2.10 code that uses macros, this might come back 
*/  

trait getSymbolCodeHeuristically {

  def apply(global: Global)(symbol: global.Symbol): List[String] = {
    
    def getStartCol(s: String) = s.indexWhere(c => c !=' ')

    val startLine = symbol.pos.line
    val source = symbol.sourceFile.toString
    val sourceText = scala.io.Source.fromFile(source).getLines
    val sourceTextLines = scala.io.Source.fromFile(source).getLines.length
    
    if (sourceTextLines < startLine) {
      println(Console.YELLOW + Console.BOLD + "symbol " + symbolWithId(global)(symbol) + " " +
                                            "has line " + startLine + " " +
                                            "for source " + source + " " +
                                            "but that source file has only " +
                                            sourceTextLines + " lines!" +
                                            Console.RESET)
      return List()    
    }
    
    var body: List[String] = sourceText.drop(startLine-1).toList
    var done = false
    
    var inBracesNest  = 0
    var inQuote       = false
    
    val initialStartCol = getStartCol(body.head)
    
    while(sourceText.hasNext && !done) {
      val line = sourceText.next
      val startCol = getStartCol(line)
      
      if (startCol > initialStartCol) {  
        
        for (char <- line) // keep track of block nesting level,
                           // in case we want to use it later
          if (!inQuote) char match {
          case '{' => inBracesNest += 1
          case '}' => inBracesNest -= 1
          case '"' => inQuote = !inQuote
          case _ =>
          }

        body = body :+ line                  // consider a line further indented as belonging
        
      }
      else if (startCol == initialStartCol) 
        if (line(startCol) == '}') {         // consider first closing brace at initial indentation column
                                             // as the last line to belong
          body = body :+ line
          done = true
        }
        else 
          done = true                        // a line that is indented same as the initial indentation
                                             // indentation, but doesn't brace-close it, means we're done
                                             // (e.g. think a case class without an explicit body)
    }
    body
    
  }
}