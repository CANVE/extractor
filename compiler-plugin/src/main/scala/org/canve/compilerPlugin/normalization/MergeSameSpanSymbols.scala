package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
import scala.tools.nsc.Global
//import pprint._, pprint.Config.Colors._ 

object MergeSameSpanSymbols {
  def apply(extractedModel: ExtractedModel) = {
    
    def pointOrStart(location: MaybePosition): Option[Int] = location match {
      case Span(start, end) => Some(start)
      case Point(loc)       => Some(loc)
      case NoPosition   => None
    }
    
    def getLocation(e: ExtractedCode) = e.codeLocation.position
    
    def extractedCode(e: ExtractedSymbol): Option[ExtractedCode] = extractedModel.codes.get.get(e.symbolCompilerId)
    
    def logDeduplication(
      extractedSymbols: (ExtractedSymbol, ExtractedSymbol),
      extractedCodes: (ExtractedCode, ExtractedCode)) = {
        val message = 
          "deduplicating symbol pair:\n" +
          List(extractedSymbols._1, extractedSymbols._2).zip(List(extractedCodes._1, extractedCodes._2)).mkString("\n")
        
        println(message)
    }
    
    /* 
     * group extracted symbols by their qualified ID    
     */
    val symbolsByQualifiedId = 
      extractedModel.graph.vertexIterator
      .filter(v => v.data.notSynthetic && v.data.definingProject == ProjectDefined)
      .toList.groupBy(m => m.data.qualifiedId)
      .foreach { groupedByQualifiedId => val group: List[ManagedExtractedSymbol] = groupedByQualifiedId._2
        
        if (group.size > 2) {
          println(group.size)
          group.foreach { x =>
            println(x.data)
            println(extractedCode(x.data))
          }
          throw DataNormalizationException(s"Unexpected amount of mergeable symbols for single source location: $groupedByQualifiedId")
        }
        
        if (group.size == 2) {
          
          val extractedSymbols: (ExtractedSymbol, ExtractedSymbol) = 
            (group.head.data, group.last.data)
            
          val extractedCodes: (Option[ExtractedCode], Option[ExtractedCode]) = 
            (extractedCode(extractedSymbols._1), extractedCode(extractedSymbols._2)) 
          
          /*
           * do the symbols sharing the same qualified ID come from the exact same source code 
           * definition? we check that by comparing their start position in the source file, 
           * treating the case of a singular position as a start position.
           */
          extractedCodes match {
            case (Some(code1), Some(code2)) =>
              val positions = (getLocation(code1), getLocation(code2))
              
              if (pointOrStart(positions._1) == pointOrStart(positions._2)) {
                logDeduplication(extractedSymbols, (code1, code2))
                
                /* 
                 * Merges a list of vertices to its head vertex, collapsing all their
                 * edges to the head vertex. 
                 * 
                 * Specifically the following steps are taken in order:
                 * 
                 * - remove all edges connecting between them
                 * - re-wire all their edges to the head vertex
                 * - remove them all, leaving only the head vertex 
                 */
                def mergeVertices(group: List[ManagedExtractedSymbol]) = 
                  group.reduce { (s1, s2) => 
                    extractedModel.graph -= extractedModel.graph.edgesBetween(s1, s2)
                    extractedModel.graph.vertexEdges(s2.key) foreach (e => 
                      extractedModel.graph.edgeReWire(e, from = s1.key, to = s2.key)
                    )
                    extractedModel.graph -= s2
                    s1
                  }
                
                mergeVertices(group)
              }
            case _ =>
          }
        }
      }
  }
}