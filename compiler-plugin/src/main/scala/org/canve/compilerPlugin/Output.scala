package org.canve.compilerPlugin
import org.canve.shared.DataIO

object Output {
  
  println(PluginArgs.outputPath.dataDir.toString)
  val dataOutputDir = new DataIO(PluginArgs.outputPath.dataDir.toString)
  println("dataOutputDir " + dataOutputDir)
  def quote(int: Int) = "\"" + int.toString() + "\""
  
  def write(implicit extractedModel: ExtractedModel) = {
    Log(s"writing extracted type relations and call graph for project ${PluginArgs.projectName}...")
    
    dataOutputDir.writeOutputFile(PluginArgs.projectName, "symbols", 
                    "implementation,name,kind,id,nonSynthetic,isParameterAccessor,isParameter,isTypeParameter,isSetter,isGetter,codeLocation,qualifyingPath,signature\n" +
                    (extractedModel.graph.vertexIterator.map(_.data.toCsvRow)).mkString("\n"))

    dataOutputDir.writeOutputFile(PluginArgs.projectName, "relations", 
        "id1,relation,id2\n" +
        extractedModel.graph.edgeIterator.map ( e => 
          List(e.v1, e.data, e.v2).mkString(","))
          .mkString("\n"))
          
    extractedModel.codes.get.foreach { keyVal => 
      val extractedCode = keyVal._2
      if (extractedCode.code.isDefined)
        dataOutputDir.writeOutputFile(
          PluginArgs.projectName, 
          keyVal._1.toString() /* extractedSymbol.qualifiedId.pickle */, 
          "< definition from source file: " + extractedCode.location.path + " >\n\n" + extractedCode.code.mkString + "\n"
        )
    }
  }
}