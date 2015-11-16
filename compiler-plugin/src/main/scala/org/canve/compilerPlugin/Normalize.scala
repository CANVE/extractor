package org.canve.compilerPlugin
import org.canve.util.CanveDataIO._
import com.github.tototoshi.csv._
import java.io.File
import org.canve.simpleGraph._

abstract class QualifiedGraphTypes {
  type QualifiedGraph = org.canve.simpleGraph.SimpleGraph[QualifiedID, RelationKind, GraphNode, GraphEdge]

  type RelationKind = String
  case class GraphEdge(id1: QualifiedID, id2: QualifiedID, data:RelationKind) extends AbstractEdge[QualifiedID, RelationKind]
  case class GraphNode(data: ExtractedSymbol) extends AbstractVertex[QualifiedID] { 
    val key = data.qualifiedId 
  }      
}

trait dataReader {
  
  case class ReadProjectData(projectName: String, nodeList: List[ExtractedSymbol], edgeList: List[ExtractedSymbolRelation])
  
  /*
   * builds canve nodes and edges from a canve data directory
   */
  def readCanveDirData(dir: File) = {
     
    val projectName = dir.getName
    
    println(s"reading data files for project $projectName from directory $dir") 
    
    val nodes: List[ExtractedSymbol] = 
      CSVReader.open(new File(dir + File.separator + "symbols")).allWithHeaders
      .map(rowMap => SymbolFromCsvRow(projectName, rowMap)) 
    
    val edges: List[ExtractedSymbolRelation] = 
      CSVReader.open(new File(dir + File.separator + "relations")).allWithHeaders
      .map(rowMap => SymbolRelationFromCsvRow(rowMap))
      
    ReadProjectData(projectName, nodes, edges)
  }
}

case class DataNormalizationException(errorText: String) extends Exception 

class Normalize extends QualifiedGraphTypes with dataReader {
  
  def assertSimilarity(s1: ExtractedSymbol, s2: ExtractedSymbol) {
    if (s1.name == s2.name) 
    if (s1.kind == s2.kind) 
    if (s1.notSynthetic == s2.notSynthetic) 
    if (s1.qualifiedId == s2.qualifiedId) 
    return
     
    throw DataNormalizationException(
      "Two symbols having the same Qualified ID are different:" +
      s"\n$s1" +  
      s"\n$s2" +  
      "This is likely a data normalization internal error") 
  }
  
  def apply: QualifiedGraph = {
    
    val projectsRawData: Iterator[ReadProjectData] = 
      getSubDirectories(canveRoot).toIterator.map(readCanveDirData)
      
    val normalizedGraphBuilder = new QualifiedGraph
    
    projectsRawData.foreach { projectRawData =>
      projectRawData.nodeList.foreach { symbol => 
        
        lazy val newVertex  = GraphNode(symbol)
        val sameKeyedVertex = normalizedGraphBuilder.vertex(symbol.qualifiedId) 
        
        sameKeyedVertex match {
          
          case None => normalizedGraphBuilder += newVertex
          
          case Some(sameKeyedVertex: GraphNode) =>
            
            assertSimilarity(sameKeyedVertex.data, symbol)
            
            (sameKeyedVertex.data.definingProject, symbol.definingProject) match {
            
            case (ExternallyDefined, ProjectDefined) => 
              normalizedGraphBuilder -= sameKeyedVertex.key += newVertex
              
            case (ProjectDefined, ExternallyDefined) => // do nothing
              
            case (ExternallyDefined, ExternallyDefined) => // do nothing
              
            case (ProjectDefined, ProjectDefined) => 
              // TODO: this exception message doesn't help zoom on the project names
              throw DataNormalizationException(
                "Encountered an ambiguous qualified symbol id - two projects define the same qualified id:" +
                s"\n$sameKeyedVertex and" +
                s"\n$newVertex") 
          }
        }
      }
    }
    
    normalizedGraphBuilder
  }
}