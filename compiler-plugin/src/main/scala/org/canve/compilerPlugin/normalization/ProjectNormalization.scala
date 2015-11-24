/*

package org.canve.compilerPlugin
import org.canve.util.CanveDataIO._
import com.github.tototoshi.csv._
import java.io.File
import org.canve.simpleGraph._

class ProjectNormalizedData extends QualifiedGraphTypes with dataReader {
  
  def assertSimilarity(s1: ExtractedSymbol, s2: ExtractedSymbol) {
    if (s1.name == s2.name) 
    if (s1.kind == s2.kind) 
    if (s1.notSynthetic == s2.notSynthetic) 
    if (s1.qualifiedId == s2.qualifiedId) 
    return
     
    println(DataNormalizationException(
      "Two symbols having the same Qualified ID are different:" +
      s"\n$s1" +  
      s"\n$s2" +  
      "\nThis is likely a canve data normalization internal error.")) 
  }
  
  private def normalize: QualifiedGraph = {
    
    val projectsRawData: Iterator[ReadProjectData] = 
      getSubDirectories(canveRoot).toIterator.map(readCanveDirData)

    projectsRawData.foreach { projectRawData =>
      
      val normalizedProjectGraph = new QualifiedGraph
      
      projectRawData.nodeList.foreach { symbol =>
        
          normalizedProjectGraph.vertex(symbol.qualifiedId) match {
            case None => normalizedProjectGraph += GraphNode(symbol)
            case Some(sameKeyed: GraphNode) =>
              if (math.abs(sameKeyed.data.id - symbol.id) == 1)   
          
          case None => normalizedGraphBuilder += newVertex
          
          case Some(sameKeyedVertex: GraphNode) =>
            
            assertSimilarity(sameKeyedVertex.data, symbol)
            
            (sameKeyedVertex.data.definingProject, symbol.definingProject) match {
            
            case (ExternallyDefined, ProjectDefined) => 
              normalizedGraphBuilder -= sameKeyedVertex.key += newVertex
              println("deduplicated one node")
              
            case (ProjectDefined, ExternallyDefined) => // do nothing
              
            case (ExternallyDefined, ExternallyDefined) => // do nothing
              
            case (ProjectDefined, ProjectDefined) => 
              // TODO: this exception message doesn't help zoom on the project names
              println(DataNormalizationException(
                "Encountered an ambiguous qualified symbol id - two symbols share the same qualified id:" +
                s"\n$sameKeyedVertex and" +
                s"\n$newVertex"))
          }
        }
      }
    }
    
    normalizedGraphBuilder
  }
 
  normalize
}

*/