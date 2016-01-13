package org.canve.shared.Sbt

import org.canve.shared.DataIO
import java.io.{File}
import org.canve.shared.ReadyOutFile
import org.canve.shared.Execution._
import org.canve.shared.Execution.TaskResultType
import org.canve.shared.DataIO._

case class Project(projectDirObj: java.io.File) {
  val name: String = projectDirObj.getName
}

/*
 * Runs canve for an sbt project located in a given directory,
 * by adding the canve sbt plugin to the project's sbt setup.
 * 
 * Arguments to the app may constrict execution, as commented inline.   
 */
object ApplyPlugin {
   
  def canApply(project: Project) = isSbtProject(project)
  
  private def isSbtProject(project: Project): Boolean = {

    val sbtProjectDir = project.projectDirObj.toString + File.separator + "project"
   
    /* heuristically determines whether it is an sbt project */
    new File(project.projectDirObj + "build.sbt").exists ||
    new File(sbtProjectDir).exists
  }
  
  /*
   * runs `sbt canve` over a given project, first adding the canve sbt plugin to the project's sbt setup
   * for that sake.  
   */
  def apply(project: Project): TimedExecutionReport[TaskResultType] = {

    val sbtProjectDir = project.projectDirObj.toString + File.separator + "project"
    
    /*
     * add the plugin to the project's sbt setup
     */

    if (!project.projectDirObj.exists)
      throw new Exception(s"Cannot apply the canve sbt plugin to directory ${project.projectDirObj.toString} - the directory does not exist")
    
    if (!isSbtProject(project)) 
      throw new Exception(s"Will not apply the canve sbt plugin to directory ${project.projectDirObj.toString} as could not determine it is an sbt project")
 
    scala.tools.nsc.io.File(ReadyOutFile(sbtProjectDir, "canve.sbt"))
      .writeAll("""addSbtPlugin("canve" % "sbt-plugin" % "0.0.1")""" + "\n")      
     
    /*
     *  run sbt for the project and check for success exit code
     */
    
    val outStream = new FilteringOutputWriter(ReadyOutFile("out", project.name + ".out"), (new java.util.Date).toString)
    
    val result = TimedExecution {
      import scala.sys.process._
      
      Process(
        Seq("sbt", "-Dsbt.log.noformat=true", "canve"), // may alternatively use the more recent -no-color instead of -Dsbt.log.... 
        project.projectDirObj) ! outStream == 0 match {
          case true  => Okay
          case false => Failure
      }
    }
    
    outStream.close
    
    result
  }
}
