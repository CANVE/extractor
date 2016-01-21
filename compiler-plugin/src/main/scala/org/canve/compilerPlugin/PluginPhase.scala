package org.canve.compilerPlugin
import scala.collection.{SortedSet, mutable}
import scala.tools.nsc.{Global, Phase}
import tools.nsc.plugins.PluginComponent

class PluginPhase(val global: Global)
                  extends PluginComponent with voidSourceFilter
                  { t =>

  import global._

  val runsAfter = List("typer")

  override val runsRightAfter = Some("typer")
  
  val phaseName = "canve-extractor"

  def units = global.currentRun
                    .units
                    .toSeq
                    .sortBy(_.source.content.mkString.hashCode())

  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def run() {
      
      val projectName = PluginArgs.projectName

      val model: ExtractedModel = new ExtractedModel(t.global) 
      
      Log("extraction starting for project " + projectName + " (" + units.length + " compilation units)")
      
      Log(t.global.currentSettings.toString) // TODO: remove or move to new compiler plugin dedicated log file
      
      units.foreach { compilationUnit =>
        if (compilationUnit.source.path.endsWith(".scala")) {
          
          if (sourceFilter(compilationUnit.source.path)) {
            Log("examining source file" + compilationUnit.source.path + "...")
            TraversalExtractionWriter(t.global)(compilationUnit, projectName, model)
            Log("done examining source file" + compilationUnit.source.path + "...")
            
            Log(model.graph.vertexCount + " symbols so far extracted for project " + projectName)
            Log(model.graph.edgeCount + " symbol relations so far extracted for project " + projectName)
          }
          
        } else Log("skipping non-scala source file: " + compilationUnit.source.path)
      }
      
      assertSourceFilterMatched
      
      normalization.NormalizeBySpans(model)
      
      Output.write(model)
    }

    def name: String = "canve" 
  }

}

