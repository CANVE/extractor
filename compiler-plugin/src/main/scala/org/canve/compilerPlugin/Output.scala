package org.canve.compilerPlugin

import org.canve.util.CanveDataIO._

object Output {
  
  def quote(int: Int) = "\"" + int.toString() + "\""
  
  def write(graph: ExtractedModel) = {
    Log(s"writing extracted type relations and call graph for project ${PluginArgs.projectName}...")
    
    writeOutputFile(PluginArgs.projectName, "symbols", 
                    "definition,notSynthetic,id,name,kind,qualifiedId\n" +
                    (graph.symbols.get.map(_.toCsvRow)).mkString("\n"))
         
    writeOutputFile(PluginArgs.projectName, "relations", 
        "id1,relation,id2\n" +
        graph.symbolRelations.get.map { extractedEdge =>
          List(extractedEdge.symbolId1, extractedEdge.relation, extractedEdge.symbolId2).mkString(",")}.mkString("\n"))
          
    graph.codes.get.foreach { keyVal => 
      val extractedCode = keyVal._2
      if (extractedCode.code.isDefined)
        writeOutputFile(
          PluginArgs.projectName, 
          keyVal._1.toString() /* extractedSymbol.qualifiedId.pickle */, 
          "< definition from source file: " + extractedCode.sourcePath + " >\n\n" + extractedCode.code.mkString + "\n"
        )
    }
  }
}