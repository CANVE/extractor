package org.canve.compilerPlugin

import org.canve.shared.CanveDataIO._

object Output {
  
  def quote(int: Int) = "\"" + int.toString() + "\""
  
  def write(implicit extractedModel: ExtractedModel) = {
    Log(s"writing extracted type relations and call graph for project ${PluginArgs.projectName}...")
    
    writeOutputFile(PluginArgs.projectName, "symbols", 
                    "implementation,nonSynthetic,id,name,kind,codeLocation,qualifyingPath,signature\n" +
                    (extractedModel.graph.vertexIterator.map(_.data.toCsvRow)).mkString("\n"))

    println("before output")
    writeOutputFile(PluginArgs.projectName, "relations", 
        "id1,relation,id2\n" +
        extractedModel.graph.edgeIterator.map ( e => 
          List(e.v1, e.data, e.v2).mkString(","))
          .mkString("\n"))
          
    extractedModel.codes.get.foreach { keyVal => 
      val extractedCode = keyVal._2
      if (extractedCode.code.isDefined)
        writeOutputFile(
          PluginArgs.projectName, 
          keyVal._1.toString() /* extractedSymbol.qualifiedId.pickle */, 
          "< definition from source file: " + extractedCode.location.path + " >\n\n" + extractedCode.code.mkString + "\n"
        )
    }
  }
}