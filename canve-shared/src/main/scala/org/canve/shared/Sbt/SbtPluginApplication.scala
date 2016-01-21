package org.canve.shared.Sbt

import org.canve.shared.DataIO
import java.io.{File}
import org.canve.shared.ReadyOutFile
import org.canve.shared.Execution._
import org.canve.shared.Execution.TaskResultType
import org.canve.shared.DataIO._
import org.canve.shared.DataWithLog
import scala.reflect.io.Directory
import scala.util.{Try, Success, Failure}
import org.canve.shared.DataWithLog
import org.canve.logging.loggers._
import play.api.libs.json._

object SbtPluginApplication {
   
  def canApply(projectClone: Directory) = isSbtProject(projectClone)
  
  /* heuristically determine whether the project is an sbt project */
  private def isSbtProject(projectClone: Directory): Boolean = {

    //println(Console.BLUE + projectClone + Console.RESET)
    val sbtProjectDir = projectClone + File.separator + "project"
   
    /* heuristically determines whether it is an sbt project */
    new File(projectClone + File.separator + "build.sbt").exists ||
    new File(sbtProjectDir).exists
  }
  
  /*
   * Runs canve for an sbt project located in a given directory,
   * by adding the canve sbt plugin to the project's sbt setup.
   * 
   * Arguments to the app may constrict execution, as commented inline.   
   */
  def apply(projectClone: Directory, outputDir: DataWithLog) = {
    
    /* logging object */
    object DataLog extends JsonLogger(outputDir.dataDir.toString) {
      def apply(logName: String, success: Boolean, reason: String) {
        DataLog(
          logName, 
          success match {
            case true  => Json.parse(s""" { "completion" : "normal" } """)
            case false => Json.parse(s""" { "completion" : "abnormal", "reason" : "$reason" } """)
          }
        )
      }
    }
   
    /*
     * add the plugin to the project's sbt setup
     */

    val sbtProjectDir = projectClone + File.separator + "project"
    
    if (!projectClone.exists)
      throw new Exception(s"Cannot apply the canve sbt plugin to directory $projectClone - the directory does not exist")
    
    if (!isSbtProject(projectClone)) 
      throw new Exception(s"Will not apply the canve sbt plugin to directory $projectClone as could not determine it is an sbt project")
 
    scala.tools.nsc.io.File(ReadyOutFile(sbtProjectDir, "canve.sbt"))
      .writeAll("""addSbtPlugin("canve" % "sbt-plugin" % "0.0.1")""" + "\n")      
     
    /*
     *  run sbt for the project and check for success exit code
     */
      
    val outStream = new FilteringOutputWriter(ReadyOutFile(outputDir.logDir, "sbt.out"), (new java.util.Date).toString)
    
    val executionReport = TimedExecution { 
      scala.sys.process.Process(
        Seq("sbt", "-Dsbt.log.noformat=true", s"canve ${outputDir.base}"), // may alternatively use the more recent -no-color instead of -Dsbt.log.... 
        new File(projectClone.toString)) ! outStream == 0  
    }
    
    outStream.close
    
    DataLog("sbt", executionReport.result, "sbt returned non-zero")
    //DataLog("
      
    executionReport      
  }
}


