package org.canve.compilerPlugin
import org.canve.util.CanveDataIO._
import com.github.tototoshi.csv._
import java.io.File
import org.canve.simpleGraph._

class NormalizeOld {
    
  type ProjectName = String
  type RelationKind = String
  
  case class IndexableNode(data: ExtractedSymbol) extends AbstractVertex[QualifiedID] { 
    val key = data.qualifiedId 
  }    
  
  case class GraphEdge(id1: QualifiedID, id2: QualifiedID, data:RelationKind) extends AbstractEdge[QualifiedID, RelationKind]
  
  type ConvergedGraph = org.canve.simpleGraph.SimpleGraph[QualifiedID, RelationKind, IndexableNode, GraphEdge]
  
  private def normalizeEdges(graph: ManagedGraph, from: Int, to: Int) {
  
  }
  
  /*
   * read canve nodes and edges from a directory
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
    
    (projectName, nodes, edges)
    //new Graph(nodes.map(GraphNode().toSet, edges.map(edge => GraphEdge(edge.symbolID1, edge.symbolID2, edge.edgeKind)).toSet)
  }
  
  val projectsRawData: Iterator[(String, List[ExtractedSymbol], List[ExtractedSymbolRelation])] = getSubDirectories(canveRoot).toIterator.map(readCanveDirData)
  /*
  projectsRawData.reduce { (graphA, graphB) =>
    
    val R = new Graph(Set(), Set())
    
    val MergedNodeList = List.newBuilder[ExtractedSymbol]
    
    val AggregateNodesList: List[ManagedGraphNode] = graphA.vertexIterator.toList ++ graphB.vertexIterator.toList 
    val groupedBy = AggregateNodesList.groupBy(_.data.qualifiedId)
    groupedBy map { case(qualifiedId, nodes) => 
      (nodes.size == 1) match {
        
        /*
         * no overlap for this qualified id,
         * so just add the only node that has it 
         */
        case true => 
          R += nodes.head
        
        /*  
         * dealing with the various cases where the two projects 
         * both have the same node, the qualified id being
         * the criteria for sameness.
         */
        case false => 
          assert(nodes.size == 2)
          val nodeA = nodes.head 
          val nodeB = nodes.tail.head
        
          /*
           * if one of the couple having the same qualified id 
           * is project-defined and the other externally-defined,
           * normalize to the project-defined one. 
           */
          (nodeA.data.definingProject, nodeB.data.definingProject) match {
            
            case (ProjectDefined, ExternallyDefined) =>
              R += nodeA
              // normalizeEdges(in = graphB, to = nodeA.id, from = nodeB.id)
              
            case (ExternallyDefined, ProjectDefined) =>
              R += nodeB
              // normalizeEdges(in = graphA, to = nodeB.id, from = nodeA.id)
              
            case (ExternallyDefined, ExternallyDefined)  =>
              R += nodeA // and no need to normalize edges
              
            case (ProjectDefined, ProjectDefined) =>
              assert(false) 

          }
      }
    }
      
    R
    //rawGraph(R.result, graphB.edges)  
  }
  
	*/
    //val merged = groupedByQualifiedId map { _._2.     
}