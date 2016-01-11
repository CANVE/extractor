package org.canve.shared.Sbt

import org.canve.shared.CanveDataIO
import java.io.{File}
import org.canve.shared.ReadyOutFile
import org.canve.shared.Execution._
import org.canve.shared.Execution.TaskResultType

case class Project(dirObj: java.io.File, name: String)

/*
 * Runs canve for an sbt project located in a given directory,
 * by adding the canve sbt plugin to the project's sbt setup.
 * 
 * Arguments to the app may constrict execution, as commented inline.   
 */
object ApplyPlugin {
  
  /*
   * runs `sbt canve` over a given project, first adding the canve sbt plugin to the project's sbt setup
   * for that sake.  
   */
  def apply(project: Project) = {

    /*
     * add the plugin to the project's sbt setup
     */
    
    val sbtProjectDir = project.dirObj.toString + File.separator + "project"
    
    scala.tools.nsc.io.File(ReadyOutFile(sbtProjectDir, "canve.sbt"))
      .writeAll("""addSbtPlugin("canve" % "sbt-plugin" % "0.0.1")""" + "\n")      
     
    /*
     *  run sbt for the project and check for success exit code
     */
    
    val outStream = new FilteringOutputWriter(ReadyOutFile("out", project.name + ".out"), (new java.util.Date).toString)
    
    val result = TimedExecution {
      import scala.sys.process._
      Process(Seq("sbt", "-Dsbt.log.noformat=true", "canve"), project.dirObj) ! outStream == 0 match {
        case true  => Okay
        case false => Failure
      }
    }
    
    outStream.close
    
    result
  }
}
