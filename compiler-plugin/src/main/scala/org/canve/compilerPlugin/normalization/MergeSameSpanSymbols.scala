package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
import scala.tools.nsc.Global

object MergeSameSpanSymbols {
  def apply(extractedModel: ExtractedModel) = {
    
    def pointOrStart(location: Location): Option[Int] = location match {
      case Span(start, end) => Some(start)
      case Point(loc)       => Some(loc)
      case NoLocationInfo   => None
    }
    
    def getLocation(e: ExtractedCode) = e.location
    
    def code(e: ExtractedSymbol): Option[ExtractedCode] = extractedModel.codes.get.get(e.symbolCompilerId)
    
    /*
     * group extracted code elements by their start position or singular location property
     */
    val symbolsByQualifiedId = extractedModel.graph.vertexIterator.filter(_.data.notSynthetic).toList.groupBy(m => m.data.qualifiedId)
    .foreach { e => val list: List[ManagedExtractedSymbol] = e._2
      if (list.size > 2) 
        throw DataNormalizationException(s"Unexpected amount of mergeable symbols for single source location: $e")
      
      if (list.size == 2) {
        val extractedSymbol1 = list.head.data
        val extractedSymbol2 = list.last.data
        (code(extractedSymbol1), code(extractedSymbol2)) match {
          case (Some(code1), Some(code2)) =>
            val location1 = getLocation(code1)
            val location2 = getLocation(code2)
            if (pointOrStart(location1) == pointOrStart(location2))
              (location1, location2) match {
              case (Span(_,_), Point(_))  => println("delete duplicate span symbol") 
              case (Point(_), Span(_,_))  => println("delete duplicate span symbol")
              case (Span(_,_), Span(_,_)) => throw DataNormalizationException(s"attempt at normalizing two spans is invalid")
              case (Point(_), Point(_))   => throw DataNormalizationException(s"attempt at normalizing two spans is invalid")
            }
          case _ =>
        }
      }
    }
  }
}