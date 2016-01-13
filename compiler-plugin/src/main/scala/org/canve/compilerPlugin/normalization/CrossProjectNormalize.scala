package org.canve.compilerPlugin.normalization
import org.canve.compilerPlugin._
import org.canve.shared.DataIO._
import com.github.tototoshi.csv._
import java.io.File
import org.canve.simpleGraph._
import scala.util.{Try, Success, Failure}

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
     * load symbols file to graph 
     */
    
    val extractedSymbols: Iterable[graph.Vertex] = {
      
      val fileRows = Try(CSVReader.open(new File(dir + File.separator + "symbols")))
      
      fileRows match {
        case Failure(ex) => 
          println(s"directory $projectName has no symbols data file")
          Iterable.empty[graph.Vertex]
        case Success(rows) =>   
          rows.allWithHeaders
          .map(inputRowMap => ExtractedSymbol(projectName, inputRowMap))
          .map(s => graph.Vertex(s.symbolCompilerId, s)).toIterable
      }
      
    }
    
    /*
     * load relations file to graph
     */

    val extractedRelations = {
      
      val fileRows = Try(CSVReader.open(new File(dir + File.separator + "relations")))
    
      fileRows match {
        case Failure(ex) => 
          println(s"directory $projectName has no relations data file")
          Iterable.empty[graph.Edge]
        case Success(rows) => rows.allWithHeaders
          .map(inputRowMap => graph.Edge(
            inputRowMap("id1").toInt, 
            inputRowMap("id2").toInt, 
            inputRowMap("relation").toString))   
        }
    }    

    graph ++ extractedSymbols
    graph ++ extractedRelations  
      
    graph
  }
}

object CrossProjectNormalizer extends DataReader with MergeStrategies {
  
  def normalize(path: Option[String] = None): ReIndexedGraph = {
    
    val dataRootPath = path match {
      case None => canveRoot  // suitable for a run as part of the sbt plugin
      case Some(path) => path // suitable for a stand-alone run
    }
    
    /* iterator of tuples (directory name, read graph) */
    val projectGraphs = 
      getSubDirectories(dataRootPath).toIterator.map(subDirectory => (subDirectory.getName, readCanveDirData(subDirectory)))

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

