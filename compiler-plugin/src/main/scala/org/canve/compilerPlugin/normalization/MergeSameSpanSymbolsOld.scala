/*

package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
import scala.tools.nsc.Global
//import pprint._, pprint.Config.Colors._ 

/*
 * Merge symbols per source file
 */

object MergeSameSpanSymbols extends MergeSameSpanSymbols

trait MergeSameSpanSymbols extends MergeStrategies {

   /* Logging classes */
  
  case class SpansDefer(extractedSymbols: (ExtractedSymbol, ExtractedSymbol))(implicit extractedModel: ExtractedModel) {
    def apply() =
      s"""(${this.getClass.getSimpleName}:) Info: samely qualified symbols have different source extraction spans:
      |${extractedSymbols._1.toJoinedString}  
      |${extractedSymbols._2.toJoinedString}""".stripMargin
  }
  
  case class StartPositionsDefer(extractedSymbols: (ExtractedSymbol, ExtractedSymbol))(implicit extractedModel: ExtractedModel) { 
    def apply() =
      s"""${this.getClass.getSimpleName}:) Info: samely qualified symbols have different source location start positions:
      |${extractedSymbols._1.toJoinedString}  
      |${extractedSymbols._2.toJoinedString}""".stripMargin    
  }
 
  /* 
   * The Merge (excuse the nested methods)
   */  
  
  def apply(implicit extractedModel: ExtractedModel) = {

    def doMerge(group: List[extractedModel.graph.Vertex], comment: Option[String] = None) = {
      assert(group.size == 2) // assumes a pair
      logMerge((group.head.data, group.last.data), comment)
      reduceSymbols(extractedModel)(group)
    }   
    
    def logMerge(extractedSymbols: (ExtractedSymbol, ExtractedSymbol), comment: Option[String]) = {
      val message = 
        s"""merging symbol pair ${comment.getOrElse("")}:
        |${extractedSymbols._1.toJoinedString}
        |${extractedSymbols._2.toJoinedString}""".stripMargin
      
      println(message)
    }
    
    /* 
     * Is a point location reasonably within bounds of given span?
     */
    def pointWithinSpan(spanStart: Int, spanEnd:Int, start:Int): Boolean = 
      spanStart <= start && start < spanEnd
     
    /* let's go */  
    
    println("merging same span symbols...")


    /*
     * Merge project-defined synthetic symbols within the current source file which:
     * have the same qualifying path, but not necessarily the same signature string
     */

    extractedModel.graph.vertexIterator // group extracted symbols - project defined synthetic ones only
    .filter(v => !v.data.nonSynthetic && v.data.implementation == ProjectDefined) 
    .toList.groupBy(v => v.data.qualifyingPath)
    .foreach { bin => val symbolBin = bin._2
  
      assert(symbolBin.forall(v => v.data.definitionCode.get.code.isEmpty)) // synthetic symbols aren't expected to have code definitions
      
      if (symbolBin.size > 2) throw 
        DataNormalizationException(s"Unexpected amount of mergeable symbols for single source location: ${symbolBin.map(_.data.toJoinedString)}")
    
      if (symbolBin.size == 2) {
        val extractedSymbols = (symbolBin.head.data, symbolBin.last.data)
        val definingCodes: (Code, Code) = (extractedSymbols._1.definitionCode.get, extractedSymbols._2.definitionCode.get)
        assert (definingCodes._1.location.path == definingCodes._1.location.path) // should come from the same source file
        doMerge(symbolBin, Some("(both synthetics)")) 
      }
    }

    /*
     * Merge project-defined non-synthetic symbols within the current source file which:
     * 
     * 1. have the same qualifying path 
     * 2. have the same signature string
     * 3. sufficiently overlap in their location of definition in the current source file
     */
    
    extractedModel.graph.vertexIterator // first off, group extracted symbols - project defined non-synthetic ones only
    .filter(v => v.data.nonSynthetic && v.data.implementation == ProjectDefined) 
    .toList.groupBy(v => FQI(v.data))
    .foreach { bin => val symbolBin = bin._2
      
      if (symbolBin.size > 2) throw 
        DataNormalizationException(s"Unexpected amount of mergeable symbols for single source location: ${symbolBin.map(_.data.toJoinedString)}")
      
      if (symbolBin.size == 2) {
        
        val extractedSymbols = (symbolBin.head.data, symbolBin.last.data)
        val definingCodes: (Code, Code) = (extractedSymbols._1.definitionCode.get, extractedSymbols._2.definitionCode.get)

        /*
         * do the symbols sharing the same FQI also share the exact same 
         * location of definition in the source code?  
         */
        assert (definingCodes._1.location.path == definingCodes._1.location.path) // should come from the same source file

        val positions: (Option[Position], Option[Position]) = (definingCodes._1.location.position, definingCodes._2.location.position)
        (positions._1, positions._2) match {
          
          // The case of two spans -> are they the same? 
          case (Some(Span(start1, end1)), Some(Span(start2, end2))) =>
            if (start1 == start2 && end1 == end2) doMerge(symbolBin, Some("non synthetics, equal spans"))
            else println(SpansDefer(extractedSymbols).apply) 

          // The case of a span and a point position -> do they share the same start position?
          case (Some(Span(spanStart, spanEnd)), Some(Point(start))) =>
            if (spanStart == start || pointWithinSpan(spanStart, spanEnd, start)) doMerge(symbolBin, Some("both non synthetics, point within span"))
            else println(StartPositionsDefer(extractedSymbols).apply) 
            
          // Same, but appearing in reverse order
          case (Some(Point(start)), Some(Span(spanStart, spanEnd))) =>
            if (spanStart == start || pointWithinSpan(spanStart, spanEnd, start)) doMerge(symbolBin.reverse, Some("both non synthetics, point within span"))
            else println(StartPositionsDefer(extractedSymbols).apply) 
            
          case x@(Some(Point(point1)), Some(Point(point2))) =>
            println(s"""Info: two symbols assumed to be the same entity both have only a position location: $x""")

          case x@_ =>
            throw DataNormalizationException(s"One or more non-synthetic symbols project-defined symbols do not have a code descriptor: $x")  
            // println(s"""Two synthetic symbols have the same Qualified ID: $x""")
            // TODO: this is aught to be when a synthetic symbol is involved. To handle later.
        }
      }
    }
  }
}
*/