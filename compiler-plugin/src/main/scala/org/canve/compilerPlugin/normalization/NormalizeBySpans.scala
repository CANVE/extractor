package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
import scala.tools.nsc.Global
import org.canve.logging.loggers._

/*
 * Merge symbols per source file
 */

object NormalizeBySpans extends NormalizeBySpans

trait NormalizeBySpans extends MergeStrategies {

  private val objectLogger = new ObjectLogger
  
  def apply(implicit extractedModel: ExtractedModel) {
    
    val paths = extractedModel.graph.vertexIterator
      .filter(_.data.codeLocation.nonEmpty).map(_.data.codeLocation.get.path).toList.distinct
    
    paths.foreach(sourceNormalize)
        
  }
  
  def sourceNormalize(path: String)(implicit extractedModel: ExtractedModel) = {

    /* utility classes/methods */
    
    case class SymbolHavingPointPosition(vertex: extractedModel.graph.Vertex, start: Int)
    case class SymbolHavingSpanPosition(vertex: extractedModel.graph.Vertex, start: Int, end: Int)
    
    /* logs merge attempt */
    def logMerge(extractedSymbols: List[ExtractedSymbol], comment: Option[String]) = {
      val message = 
        s"""merging ${extractedSymbols.length} symbols ${comment.getOrElse("")}: 
        |${extractedSymbols map (_.toJoinedString + "\n")}""".stripMargin
      
      println(message)
    }
    
    /* passes on to merge strategy */
    def mergeSpans(group: List[extractedModel.graph.Vertex]) = {
      logMerge(group map (_.data), Some("(spans)"))
      reduceSymbols(extractedModel)(group)
    }   
    
    /* passes on to merge strategy */
    def mergePointToSpan(spanSymbolVertex: extractedModel.graph.Vertex, pointSymbolVertex: extractedModel.graph.Vertex) {
      val mergeList = List(spanSymbolVertex, pointSymbolVertex)
      logMerge(mergeList map (_.data), Some("(point to span)"))
      reduceSymbols(extractedModel)(mergeList)
    }
    
    /* is a point location reasonably within bounds of given span? */
    def pointWithinSpan(spanStart: Int, spanEnd:Int, start:Int): Boolean = 
      spanStart <= start && start < spanEnd
    
    /* 
     * let's go 
     */  

    def pathVertices: Iterator[extractedModel.graph.Vertex] = 
      extractedModel.graph.vertexIterator
      .filter(_.data.implementation == ProjectDefined)
      .filter(_.data.codeLocation.nonEmpty)
      .filter(_.data.codeLocation.get.path == path)
      
    println(s"merging same span symbols for source $path...")
   
    /* get all symbols having span positions, grouped by (start, end) */
    val symbolsBySpan: Map[(Int, Int), List[SymbolHavingSpanPosition]] = 
      pathVertices
      .map(v => (v.data.codeLocation.flatMap(_.position), v))
      .collect { case (Some(Span(start,end)), v) => SymbolHavingSpanPosition(v, start, end) }
      .toList 
      .groupBy(s => (s.start, s.end))
      
    objectLogger(symbolsBySpan, "symbolsBySpan")
    
    /* 
     * merge them by qualifying path (through further grouping per qualifying path) 
     */
    symbolsBySpan.filter(_._2.tail.nonEmpty).map { spanGroup =>
      println(s"inspecting ${spanGroup._2.size} symbols sharing same span (${spanGroup}) for merge eligibility...")
      spanGroup._2.groupBy(_.vertex.data.qualifyingPath) }
    .foreach { qpMap =>
       qpMap foreach { qualifyingPathBin: (QualifyingPath, List[SymbolHavingSpanPosition]) =>
         if (qualifyingPathBin._2.tail.nonEmpty) mergeSpans(qualifyingPathBin._2.map(_.vertex))
       }
    }

    /* 
     * merge symbols having point positions, where appropriate 
     */
    
    /* further group by start position - this is deliberately a map of a map */  
    val symbolBySpanAndStart: Map[Int, Map[(Int, Int), List[SymbolHavingSpanPosition]]] = 
      pathVertices
      .map(v => (v.data.codeLocation.flatMap(_.position), v))
      .collect { case (Some(Span(start,end)), v) => SymbolHavingSpanPosition(v, start, end) }
      .toList 
      .groupBy(s => (s.start, s.end))
      .groupBy(_._1._1)

    objectLogger(symbolBySpanAndStart, "symbolBySpanAndStart")  
      
    /* all symbols having point positions */
    val points: Iterator[SymbolHavingPointPosition] =
      pathVertices
      .map(v => (v.data.codeLocation.flatMap(_.position), v))
      .collect { case (Some(Point(start)), v) => SymbolHavingPointPosition(v, start) }
    
    println("4876:" + symbolBySpanAndStart.get(4876))  
      
    points.foreach { symbolHavingPointPosition =>
      
      symbolBySpanAndStart.get(symbolHavingPointPosition.start) match {
        
        case None => 
          println(s"symbol definition associated to pointwise location does not match any other span-defined symbol (symbol: $symbolHavingPointPosition)")
          
        case Some(sameStartSpanSymbols: Map[(Int, Int), List[SymbolHavingSpanPosition]]) =>
          
          val spanMatches: Map[(Int, Int), List[SymbolHavingSpanPosition]] = 
            sameStartSpanSymbols.filter(s => pointWithinSpan(start = symbolHavingPointPosition.start, spanStart = s._1._1, spanEnd = s._1._2))
          
          if (spanMatches.tail.isEmpty)
            mergePointToSpan(spanSymbolVertex = spanMatches.toList.head._2.head.vertex, pointSymbolVertex = symbolHavingPointPosition.vertex)
          else
            println(
              s"""symbol definition associated to pointwise location matches more than one span-defined symbol: 
                 |symbol: $symbolHavingPointPosition"
                 |matching spans: $spanMatches
                 |it is therefore left ambiguous whether/how to merge it""".stripMargin)
      }
    }
  }

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
}
