package org.canve.shared.Sbt

import org.canve.shared.DataIO
import java.io.{File}
import org.canve.shared.ReadyOutFile
import org.canve.shared.Execution._
import org.canve.shared.Execution.TaskResultType
import org.canve.shared.DataIO._
import org.canve.shared.DataWithLog
import scala.reflect.io.Directory

/*
 * Runs canve for an sbt project located in a given directory,
 * by adding the canve sbt plugin to the project's sbt setup.
 * 
 * Arguments to the app may constrict execution, as commented inline.   
 */
object ApplyPlugin {
   
  def canApply(projectClone: Directory) = isSbtProject(projectClone)
  
  private def isSbtProject(projectClone: Directory): Boolean = {

    val sbtProjectDir = projectClone + File.separator + "project"
   
    /* heuristically determines whether it is an sbt project */
    new File(projectClone + File.separator + "build.sbt").exists ||
    new File(sbtProjectDir).exists
  }
  
  /*
   * runs `sbt canve` over a given project, first adding the canve sbt plugin to the project's sbt setup
   * for that sake.  
   */
  def apply(projectClone: Directory, outputDir: String): TimedExecutionReport[TaskResultType] = {

    val sbtProjectDir = projectClone + File.separator + "project"
    
    /*
     * add the plugin to the project's sbt setup
     */

    if (!projectClone.exists)
      throw new Exception(s"Cannot apply the canve sbt plugin to directory $projectClone - the directory does not exist")
    
    if (!isSbtProject(projectClone)) 
      throw new Exception(s"Will not apply the canve sbt plugin to directory $projectClone as could not determine it is an sbt project")
 
    scala.tools.nsc.io.File(ReadyOutFile(sbtProjectDir, "canve.sbt"))
      .writeAll("""addSbtPlugin("canve" % "sbt-plugin" % "0.0.1")""" + "\n")      
     
    /*
     *  run sbt for the project and check for success exit code
     */
    
    val out = new DataWithLog(outputDir)
      
    val outStream = new FilteringOutputWriter(ReadyOutFile(out.logDir, projectClone.name + ".out"), (new java.util.Date).toString)
    
    val result = TimedExecution {
      
      scala.sys.process.Process(
        Seq("sbt", "-Dsbt.log.noformat=true", s"canve ${out.dataDir}"), // may alternatively use the more recent -no-color instead of -Dsbt.log.... 
        new File(projectClone.toString)) ! outStream == 0 match {
          case true  => Okay
          case false => Failure
      }
    }
    
    outStream.close
    
    result
  }
}
