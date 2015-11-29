package org.canve.compilerPlugin

import org.canve.util.CanveDataIO._

object Output {
  
  def quote(int: Int) = "\"" + int.toString() + "\""
  
  def write(model: ExtractedModel) = {
    Log(s"writing extracted type relations and call graph for project ${PluginArgs.projectName}...")
    
    writeOutputFile(PluginArgs.projectName, "symbols", 
                    "definition,notSynthetic,id,name,kind,qualifiedId\n" +
                    (model.graph.vertexIterator.map(_.data.toCsvRow)).mkString("\n"))

    println("before output")
    writeOutputFile(PluginArgs.projectName, "relations", 
        "id1,relation,id2\n" +
        model.graph.edgeIterator.map ( e => 
          List(e.v1, e.data, e.v2).mkString(","))
          .mkString("\n"))
          
    model.codes.get.foreach { keyVal => 
      val extractedCode = keyVal._2
      if (extractedCode.code.isDefined)
        writeOutputFile(
          PluginArgs.projectName, 
          keyVal._1.toString() /* extractedSymbol.qualifiedId.pickle */, 
          "< definition from source file: " + extractedCode.codeLocation.path + " >\n\n" + extractedCode.code.mkString + "\n"
        )
    }
  }
}