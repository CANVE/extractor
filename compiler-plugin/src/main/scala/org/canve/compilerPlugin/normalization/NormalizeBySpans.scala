package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
import scala.tools.nsc.Global
import org.canve.logging.loggers._
import io.circe.Encoder

/*
 * Merge symbols per source file
 */

object NormalizeBySpans extends NormalizeBySpans

trait NormalizeBySpans extends MergeStrategies {

  //private val objectLogger = new ObjectLogger
  
  def apply(implicit extractedModel: ExtractedModel) {
    
    val paths = extractedModel.graph.vertexIterator
      .filter(_.data.codeLocation.nonEmpty).map(_.data.codeLocation.get.path).toList.distinct
    
    paths.foreach(sourceNormalize)
        
  }
  
  def sourceNormalize(path: String)(implicit extractedModel: ExtractedModel) = {

    /* utility classes/methods */
    
    abstract trait SymbolLift
    case class SymbolHavingPointPosition(vertex: extractedModel.graph.Vertex, start: Int) extends SymbolLift
    case class SymbolHavingSpanPosition(vertex: extractedModel.graph.Vertex, start: Int, end: Int) extends SymbolLift
    
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
    def mergePointToSpan(
      spanSymbolVertex: extractedModel.graph.Vertex, 
      pointSymbolVertex: extractedModel.graph.Vertex,
      message: Option[String] = Some("(point to span)")) {
      val mergeList = List(spanSymbolVertex, pointSymbolVertex)
      logMerge(mergeList map (_.data), message)
      reduceSymbols(extractedModel)(mergeList)
    }
    
    /* is a point location reasonably within bounds of given span? */
    def pointWithinSpan(spanStart: Int, spanEnd:Int, start:Int): Boolean = 
      spanStart <= start && start < spanEnd
    
    /* 
     * let's go 
     */  
    
    println(s"inter-merging symbols for source $path...")

    /* returns all symbols belonging to given path, which are project defined and have code location indication */
    def pathVertices: Iterator[extractedModel.graph.Vertex] = 
      extractedModel.graph.vertexIterator
      .filter(_.data.implementation == ProjectDefined)
      .filter(_.data.codeLocation.nonEmpty)
      .filter(_.data.codeLocation.get.path == path)
    
    def liftedPoints(vertices: Iterator[extractedModel.graph.Vertex]) = {
      val verboseSeq = vertices.map(v => (v.data.codeLocation.flatMap(_.position), v))
      verboseSeq.collect { case (Some(Point(start)), v) => SymbolHavingPointPosition(v, start) } 
    }
      
    def liftedSpans(vertices: Iterator[extractedModel.graph.Vertex]) = {
      val verboseSeq = vertices.map(v => (v.data.codeLocation.flatMap(_.position), v))
      verboseSeq.collect { case (Some(Span(start,end)), v) => SymbolHavingSpanPosition(v, start, end) } 
    }

    /* returns all which have span position */
    def symbolHavingSpanPositionIterator: Iterator[SymbolHavingSpanPosition] = liftedSpans(pathVertices)
    
    /* returns all symbols which have point position */
    def symbolsHavingPointPositionIterator: Iterator[SymbolHavingPointPosition] = liftedPoints(pathVertices)

    /* all symbols having span positions, grouped by (start, end) */
    val symbolsBySpan: Map[(Int, Int), List[SymbolHavingSpanPosition]] = 
      symbolHavingSpanPositionIterator.toList.groupBy(s => (s.start, s.end))
      
    /* merge same span symbols, that have the same qualifying path */
    symbolsBySpan.filter(_._2.tail.nonEmpty).map { spanGroup =>
      println(s"inspecting ${spanGroup._2.size} symbols sharing same span (${spanGroup}) for merge eligibility...")
      spanGroup._2.groupBy(_.vertex.data.qualifyingPath) }
    .foreach { qpMap =>
       qpMap foreach { qualifyingPathBin: (QualifyingPath, List[SymbolHavingSpanPosition]) =>
         if (qualifyingPathBin._2.tail.nonEmpty) mergeSpans(qualifyingPathBin._2.map(_.vertex))
       }
    }

    val symbolBySpanAndStart: Map[Int, Map[(Int, Int), List[SymbolHavingSpanPosition]]] = // further groups by start position - this is deliberately a map of a map  
      symbolHavingSpanPositionIterator
      .toList 
      .groupBy(s => (s.start, s.end))
      .groupBy(_._1._1)
      
    val symbolsHavingPointPosition: Iterator[SymbolHavingPointPosition] = symbolsHavingPointPositionIterator 
    
    symbolsHavingPointPosition.foreach { symbolHavingPointPosition =>
      symbolBySpanAndStart.get(symbolHavingPointPosition.start) match {
        
        /* merge symbols having point positions each with a span position symbol having the same start - if any */      
        case Some(sameStartSpanSymbols: Map[(Int, Int), List[SymbolHavingSpanPosition]]) =>
          
          val spanMatches: Map[(Int, Int), List[SymbolHavingSpanPosition]] = sameStartSpanSymbols
          
          if (spanMatches.tail.isEmpty)
            mergePointToSpan(
              spanSymbolVertex = spanMatches.toList.head._2.head.vertex, 
              pointSymbolVertex = symbolHavingPointPosition.vertex,
              Some("point starts with span"))
          else
            DataWarning(
              s"""symbol definition associated to pointwise location matches more than one span-defined symbol: 
                 |symbol: $symbolHavingPointPosition"
                 |matching spans: $spanMatches
                 |it is therefore left ambiguous whether/how to merge it""".stripMargin)
        
        case None => 
          /* merge symbols having point positions which have the same qualified path as a symbol they are part of its span */
          val SpansGroupedByQualifyingPath: Map[QualifyingPath, List[SymbolHavingSpanPosition]] = 
            liftedSpans(pathVertices)
            .toList
            .groupBy(_.vertex.data.qualifyingPath)
          
          SpansGroupedByQualifyingPath.get(symbolHavingPointPosition.vertex.data.qualifyingPath) match {
            case Some(symbolsHavingSpans) =>
              val wrappingSpans = 
                symbolsHavingSpans.filter(span => 
                  pointWithinSpan(spanStart = span.start, spanEnd = span.end, start = symbolHavingPointPosition.start))

              if (wrappingSpans.size == 0)    
                DataWarning(
                  s"""symbol definition associated to pointwise location matches one or more symbols by qualified path, but does not fall within their span
                     |symbol: $symbolHavingPointPosition
                     |symbols with span location that match in qualified name but do not wrap this symbol's position:
                     |$symbolsHavingSpans""")
                  
              if (wrappingSpans.size == 1)
                mergePointToSpan(
                  spanSymbolVertex = wrappingSpans.head.vertex, 
                  pointSymbolVertex = symbolHavingPointPosition.vertex,
                  Some("point within span"))
              
              if (wrappingSpans.size > 1)                
                DataWarning(
                  s"""symbol definition associated to pointwise location matches more than one span-defined symbol: 
                     |symbol: $symbolHavingPointPosition"
                     |matching spans: $wrappingSpans
                     |it is therefore left ambiguous whether/how to merge it""".stripMargin)
              
            case None =>
              DataWarning(s"symbol definition associated to pointwise location does not match any other span-defined symbol (symbol: $symbolHavingPointPosition)")
          }
      }
    }
    
    
  }

  //TODO: if this module stands, revive checking for the conditions these logging classes are for:
  
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

//objectLogger(symbolsBySpan, "symbolsBySpan")
//objectLogger(symbolBySpanAndStart, "symbolBySpanAndStart")
/*
    import io.circe._, io.circe.generic.auto._, io.circe.parse._, io.circe.syntax._
    import cats.data.Xor
      
    implicit object SymbolNameSerialization extends Encoder[SymbolName] {
      def apply(o: SymbolName) = o.name.asJson
    }
    
    implicit object ExtractedSymbolSerialization extends Encoder[ExtractedSymbol] {
      def apply(o: ExtractedSymbol) = o.name.asJson  
    }
    
    implicit object SymbolHavingSpanPositionSerialization extends Encoder[SymbolHavingSpanPosition] {
      def apply(o: SymbolHavingSpanPosition) = {
        o.vertex.data.asJson
      }
    }
    
    implicit object SymbolHavingSpanPositionSpecialization extends Encoder[Map[(Int, Int),List[SymbolHavingSpanPosition]]] {
      def apply(o: Map[(Int, Int),List[SymbolHavingSpanPosition]]) = {
        val toJsonKeys: Map[String, List[SymbolHavingSpanPosition]] = 
          o.map(tuple => (List(tuple._1._1, tuple._1._2).mkString(",") -> tuple._2))
        toJsonKeys.asJson
      }
    }

*/
