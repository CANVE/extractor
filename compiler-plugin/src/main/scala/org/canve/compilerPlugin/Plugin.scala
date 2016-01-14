package org.canve.compilerPlugin
import tools.nsc.Global
import scala.collection.SortedSet
import java.io.File

object PluginArgs {
  var projectName: String = ""
  var outputRootPath: String = "canve-data" // default value
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
    val outputRootPathArgPrefix = "outputDataPath:" 
    
    for ( opt <- opts ) {
      (opt.startsWith(projNameArgPrefix), opt.startsWith(outputRootPathArgPrefix)) match {
        case (true, _) =>
          PluginArgs.projectName = opt.substring(projNameArgPrefix.length)
          Log("instrumenting project " + PluginArgs.projectName + "...")
        case (_, true) =>
          PluginArgs.outputRootPath = opt.substring(outputRootPathArgPrefix.length)
          Log("data will be written under " + PluginArgs.outputRootPath + File.separator + PluginArgs.projectName) 
        case (false, false) =>
          error("Unknown invocation parameter passed to the CANVE compiler plugin: " + opt)
        case _ => throw new Exception("internal error")
      }
    }

    if (!opts.exists(_.startsWith("projectName")))
      throw new RuntimeException("canve compiler plugin invoked without a project name argument")
 
  }
}
