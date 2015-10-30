package org.canve.compilerPlugin

import org.canve.util.CanveDataIO._

object Output {
  
  def quote(int: Int) = "\"" + int.toString() + "\""
  
  def write = {
    Log("writing extracted type relations and call graph...")
    writeOutputFile(PluginArgs.projectName, "nodes",
        "definition,notSynthetic,id,name,kind\n" +
        Nodes.map.map { node =>
          List(node._2.source match {
                case Some(_) => "project"
                case None    => "external"
               },
               node._2.notSynthetic,
               node._2.id, 
               node._2.name, 
               node._2.kind)
               .mkString(",")}.mkString("\n"))
         
    writeOutputFile(PluginArgs.projectName, "edges", 
        "id1,edgeKind,id2\n" +
        Edges.set.map { edge =>
          List(edge.id1, edge.edgeKind, edge.id2).mkString(",")}.mkString("\n"))
          
    Nodes.map.map(_._2).foreach(node =>
      if (node.source.isDefined)
        writeOutputFile(PluginArgs.projectName, "node-source-" + node.id, 
                        "< in file " + node.fileName.get + " >\n\n" + node.source.get.mkString + "\n"))
  }
  
}