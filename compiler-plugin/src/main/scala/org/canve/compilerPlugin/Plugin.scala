package org.canve.compilerPlugin
import tools.nsc.Global
import scala.collection.SortedSet
import java.io.File
import org.canve.shared.DataWithLog
import org.canve.logging.loggers.StringLogger

/* compiler plugin arguments to be obtained */
object PluginArgs {
  var outputPath: DataWithLog = new DataWithLog("canve-data") // default value
  var projectName: String = ""
}  

/* object setting and providing access to a log */
object Log {
  val log = new StringLogger(PluginArgs.outputPath.logDir + File.separator + "compilerPlugin.log") 
  def apply(s: String) = log(s)
}

class RuntimePlugin(val global: Global) extends tools.nsc.plugins.Plugin {

  val name = "canve"
  val description = "extracts type relationships and call graph during compilation"

  val components = List[tools.nsc.plugins.PluginComponent](
    new PluginPhase(this.global) 
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
        case (_, true) =>
          PluginArgs.outputPath = new DataWithLog(opt.substring(outputRootPathArgPrefix.length))
          println("outpath: " + PluginArgs.outputPath)
        case (false, false) =>
          error("Unknown invocation parameter passed to the CANVE compiler plugin: " + opt)
        case _ => throw new Exception("internal error")
      }
    }
   
    if (!opts.exists(_.startsWith("projectName")))
      throw new RuntimeException("canve compiler plugin invoked without a project name argument")
    
    
    Log("instrumenting project " + PluginArgs.projectName + "...")
    //Log(s"data for project ${PluginArgs.projectName} will be written in " + PluginArgs.outputPath + File.separator + PluginArgs.projectName) 
    
  }
}
