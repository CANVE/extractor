package org.canve.compilerPlugin

import org.canve.util.CanveDataIO._

object Output {
  
  def quote(int: Int) = "\"" + int.toString() + "\""
  
  def write(graph: ExtractedGraph) = {
    Log(s"writing extracted type relations and call graph for project ${PluginArgs.projectName}...")
    
    writeOutputFile(PluginArgs.projectName, "symbols", 
                    "definition,notSynthetic,id,name,kind,qualifiedId\n" +
                    (graph.extractedSymbols.get.map(_.toCsvRow)).mkString("\n"))
         
    writeOutputFile(PluginArgs.projectName, "relations", 
        "id1,relation,id2\n" +
        graph.extractedSymbolRelations.get.map { extractedEdge =>
          List(extractedEdge.symbolID1, extractedEdge.relation, extractedEdge.symbolID2).mkString(",")}.mkString("\n"))
          
    graph.extractedSymbols.get.foreach(extractedSymbol =>
      if (extractedSymbol.sourceCode.isDefined)
        writeOutputFile(PluginArgs.projectName, extractedSymbol.qualifiedId.pickle, 
                        "< definition from source file: " + extractedSymbol.definingFileName.get + " >\n\n" + extractedSymbol.sourceCode.get.mkString + "\n"))
  }
  
}