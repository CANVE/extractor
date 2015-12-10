package org.canve.compilerPlugin
import scala.collection.{SortedSet, mutable}
import scala.tools.nsc.{Global, Phase}
import tools.nsc.plugins.PluginComponent

class PluginPhase(val global: Global)
                  extends PluginComponent with debugSourceFilter
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
      
      units.foreach { unit =>
        if (unit.source.path.endsWith(".scala")) {
          
          if (sourceFilter(unit.source.path)) {
            Log("examining source file" + unit.source.path + "...")
            TraversalExtractionWriter(t.global)(unit, projectName, model)
            Log("done examining source file" + unit.source.path + "...")
            
            Log(model.graph.vertexCount + " symbols so far extracted for project " + projectName)
            Log(model.graph.edgeCount + " symbol relations so far extracted for project " + projectName)
          }
          
        } else Log("skipping non-scala source file: " + unit.source.path)
      }
      
      normalization.NormalizeBySpans(model)
      
      Output.write(model)
    }

    def name: String = "canve" 
  }

}

abstract trait sourceFilter {
  def sourceFilter(path: String): Boolean
}

abstract trait voidSourceFilter {
  def sourceFilter(path: String) = true
}

trait debugSourceFilter extends sourceFilter {
  def sourceFilter(path: String) = path.endsWith("/pipeline/src/main/scala/org/allenai/pipeline/PipescriptParser.scala")
}