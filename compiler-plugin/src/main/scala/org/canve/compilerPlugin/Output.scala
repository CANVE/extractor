package org.canve.compilerPlugin

import org.canve.util.CanveDataIO._

object Output {
  
  def quote(int: Int) = "\"" + int.toString() + "\""
  
  def write = {
    Log(s"writing extracted type relations and call graph for project ${PluginArgs.projectName}...")
    
    writeOutputFile(PluginArgs.projectName, "symbols", 
                    "definition,notSynthetic,id,name,kind,qualifiedId\n" +
                    (ExtractedSymbols.map map (_._2.toCsvRow)).mkString("\n"))
         
    writeOutputFile(PluginArgs.projectName, "relations", 
        "id1,relation,id2\n" +
        ExtractedSymbolRelations.set.map { edge =>
          List(edge.symbolID1, edge.relation, edge.symbolID2).mkString(",")}.mkString("\n"))
          
    ExtractedSymbols.map.map(_._2).foreach(node =>
      if (node.sourceCode.isDefined)
        writeOutputFile(PluginArgs.projectName, node.qualifiedId + ".source", 
                        "< definition from source file: " + node.definingFileName.get + " >\n\n" + node.sourceCode.get.mkString + "\n"))
  }
  
}