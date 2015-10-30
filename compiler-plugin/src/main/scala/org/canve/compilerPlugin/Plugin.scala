package org.canve.compilerPlugin
import tools.nsc.Global
import scala.collection.SortedSet


object PluginArgs {
  var projectName: String = ""
}  

class RuntimePlugin(val global: Global) extends tools.nsc.plugins.Plugin {

  val name = "canve"
  val description = "extracts type relationships and call graph during compilation"

  val components = List[tools.nsc.plugins.PluginComponent](
    new PluginPhase(this.global) // TODO: is the `this` really required here?
  )
  
  /*
   * overriding a callback function called by scalac for handling scalac arguments
   */
  override def processOptions(opts: List[String], error: String => Unit) {
    val projNameArgPrefix = "projectName:"
    
    for ( opt <- opts ) {
      if (opt.startsWith(projNameArgPrefix)) {
        PluginArgs.projectName = opt.substring(projNameArgPrefix.length)
        Log("instrumenting project " + PluginArgs.projectName + "...") 
      }
      else
        error("Unknown invocation parameter passed to the CANVE compiler plugin: " + opt)
    }
    if (!opts.exists(_.startsWith("projectName")))
      throw new RuntimeException("canve compiler plugin invoked without a project name argument")
  }
}
