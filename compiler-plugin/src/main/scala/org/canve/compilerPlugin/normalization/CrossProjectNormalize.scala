package org.canve.compilerPlugin
import org.canve.util.CanveDataIO._
import com.github.tototoshi.csv._
import java.io.File
import org.canve.simpleGraph._

/*
 * Aggregate symbols across projects, while de-duplicating
 * symbols defined in one project and used in the other
 */

trait DataReader {

  type LoadedGraph = SimpleGraph[SymbolCompilerId, ExtractedSymbol, ExtractedSymbolRelation]
  type ReIndexedGraph = SimpleGraph[QualifiedID, ExtractedSymbol, ExtractedSymbolRelation]
  
  /*
   * builds canve nodes and edges from a canve data directory
   */
  def readCanveDirData(dir: File): ManagedExtractedGraph = {
     
    val projectName = dir.getName
    
    println(s"reading data files for project $projectName from directory $dir")
    
    val graph = new LoadedGraph

    /*
     * load symbol file to graph 
     */
    graph ++ {
      
      val symbols: List[ExtractedSymbol] = 
        CSVReader.open(new File(dir + File.separator + "symbols")).allWithHeaders
        .map(inputRowMap => SymbolFromCsvRow(projectName, inputRowMap))
        
      val asVertices: Iterable[graph.Vertex] = symbols.map(s => graph.Vertex(s.symbolCompilerId, s)).toIterable
      
      asVertices
    }
    
    /*
     * load symbol relation file to graph
     */
    CSVReader.open(new File(dir + File.separator + "relations")).allWithHeaders
      .map(inputRowMap => graph.addIfUnique(graph.Edge(inputRowMap("id1").toInt, inputRowMap("id2").toInt, inputRowMap("relation").toString)))
      
    graph
  }
}

object CrossProjectNormalizer extends DataReader {
  
  def apply = normalize
  
  def normalize: ReIndexedGraph = {
    
    val projectGraphs: Iterator[LoadedGraph] = 
      getSubDirectories(canveRoot).toIterator.map(readCanveDirData)

    val aggregateGraph = new ReIndexedGraph 
    
    projectGraphs.foreach { graph =>
      graph.vertexIterator.foreach { v => val symbol = v.data
        aggregateGraph.vertex(symbol.qualifiedId) match {
        
          case None => aggregateGraph ++ aggregateGraph.Vertex(key = symbol.qualifiedId, data = symbol)
          
          case Some(v) => maybeMerge(aggregateGraph, aggregateGraphSymbol = v.data, sameKeyedSymbol = symbol)
          
        }
      }
    }

    aggregateGraph  
  }
  
  private def maybeMerge(
    aggregateGraph: ReIndexedGraph, 
    aggregateGraphSymbol: ExtractedSymbol, 
    sameKeyedSymbol: ExtractedSymbol) = {
    
    assertSimilarity(aggregateGraphSymbol, sameKeyedSymbol)
    
    (aggregateGraphSymbol.definingProject, sameKeyedSymbol.definingProject) match {

      case (ProjectDefined, ExternallyDefined) => // do nothing
      case (ExternallyDefined, ExternallyDefined) => // do nothing
            
      case (ExternallyDefined, ProjectDefined) => 
        aggregateGraph -- aggregateGraphSymbol.qualifiedId 
        aggregateGraph ++ aggregateGraph.Vertex(key = sameKeyedSymbol.qualifiedId, data = sameKeyedSymbol)
        println("deduplicated one node")
              
      case (ProjectDefined, ProjectDefined) => 
        // TODO: This exception message doesn't help zoom in on the project names without logs excavation.
        //       Might be solved by adding the project name to each symbol being compared, somewhere before. 
        println(DataNormalizationException(
          "Two subprojects seem to define the same qualified name:" + 
          s"\n$aggregateGraphSymbol and" +
          s"\n$sameKeyedSymbol"))
    }
  }
  
  private def assertSimilarity(s1: ExtractedSymbol, s2: ExtractedSymbol) {
    if (s1.name == s2.name) 
    if (s1.kind == s2.kind) 
    if (s1.notSynthetic == s2.notSynthetic) 
    if (s1.qualifiedId == s2.qualifiedId) 
    return
     
    println(DataNormalizationException(
      s"""Two symbols having the same Qualified ID are unexpectedly different:
      |$s1" +  
      |$s2" +  
      |This may indicate the normalization model is relying on a false assumption.""".stripMargin)) 
  }
}
