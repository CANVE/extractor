package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
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
  type ReIndexedGraph = SimpleGraph[FQI, ExtractedSymbolPlus, ExtractedSymbolRelation]
  
  /*
   * builds graph from a canve data directory
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

object CrossProjectNormalizer extends DataReader with MergeStrategies {
  
  def apply = normalize
  
  def normalize: ReIndexedGraph = {
    
    /* iterator of tuples (directory name, read graph) */
    val projectGraphs = 
      getSubDirectories(canveRoot).toIterator.map(subDirectory => (subDirectory.getName, readCanveDirData(subDirectory)))

    val aggregateGraph = new ReIndexedGraph 
    
    // TODO: make / refactor this to avoid ever merging stuff from same project
    
    projectGraphs.foreach { iterator => 
      
      val (projectName, graph) = iterator
            
      graph.vertexIterator.foreach { v => val symbol = ExtractedSymbolPlus(v.data, projectName)
        aggregateGraph.vertex(FQI(symbol)) match {
        
          case None => aggregateGraph ++ aggregateGraph.Vertex(key = FQI(symbol), data = symbol)
          
          case Some(v) => maybeMerge(aggregateGraph, aggregateGraphSymbol = v.data, sameKeyedSymbol = symbol)
          
        }
      }
    }

    aggregateGraph  
  }
  
  /*
   * 
   */
  private def maybeMerge(
    aggregateGraph: ReIndexedGraph, 
    aggregateGraphSymbol: ExtractedSymbolPlus, 
    sameKeyedSymbol: ExtractedSymbolPlus) = {
    
    assertSimilarity(aggregateGraphSymbol, sameKeyedSymbol)
    
    (aggregateGraphSymbol.implementation, sameKeyedSymbol.implementation) match {

      case (ProjectDefined, ExternallyDefined) => 
        // do not add new symbol to aggregate graph, thus de-duplicating it
        // TODO: !! handle its edges re-wire though!
        println(s"deduplicated one symbol: $sameKeyedSymbol")
        
      case (ExternallyDefined, ProjectDefined) => 
        // replace externally defined symbol with the new one, thus de-duplicating it
        aggregateGraph -- FQI(aggregateGraphSymbol) 
        aggregateGraph ++ aggregateGraph.Vertex(key = FQI(sameKeyedSymbol), data = sameKeyedSymbol)
        // TODO: handle the edges re-wire! 
        println(s"deduplicated one symbol: $aggregateGraphSymbol")

      case (ExternallyDefined, ExternallyDefined) => // do nothing, or rather merge to single ExternallyDefined one?       
        
      case (ProjectDefined, ProjectDefined) =>  
        // TODO: This exception message doesn't help zoom in on the project names without logs excavation.
        //       Might be solved by adding the project name to each symbol being compared, somewhere before. 
        println(DataNormalizationException(
          "Two symbols seem to define the same qualified name:" + 
          s"\n$aggregateGraphSymbol and" +
          s"\n$sameKeyedSymbol"))
    }
  }
  
  private def assertSimilarity(s1: ExtractedSymbol, s2: ExtractedSymbol) {
    if (s1.name == s2.name) 
    if (s1.kind == s2.kind) 
    if (s1.nonSynthetic == s2.nonSynthetic) 
    if (s1.qualifyingPath == s2.qualifyingPath) 
    return
     
    println(DataNormalizationException(
      s"""Two symbols expected to be the same are unexpectedly different:
      |$s1 
      |$s2  
      |This may indicate the normalization model is relying on a false assumption.""".stripMargin)) 
  }
}
