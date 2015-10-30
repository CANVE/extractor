package org.canve.sbtPluginTestLib

import org.canve.util.CanveDataIO
import java.io.{File}
import scala.sys.process._
import util.TimedExecution
import util.ReadyOutFile

/*
 * Runs canve for each project included under the designated directory, 
 * by adding the canve sbt plugin to the project's sbt setup.
 * 
 * Arguments to the app may constrict execution, as commented inline.   
 */
object Runner extends App {
  
  //val testProjectsRoot = "test-projects"
  val testProjectsRoot: String = getClass.getResource("/integration-test-projects").getFile
  
  println(new File(".").getAbsolutePath)
  println(CanveDataIO.getSubDirectories(testProjectsRoot))
  val results = CanveDataIO.getSubDirectories(testProjectsRoot) map { projectDirObj =>
    val project = Project(projectDirObj, projectDirObj.getName)
    
    /*
     * if there's no main args provided execute all tests,
     * otherwise be selective according to a first main arg's value
     */
    if ((args.isEmpty) || (args.nonEmpty && project.name.startsWith(args.head))) 
    {    
      
      val projectPath = testProjectsRoot + File.separator + project.name 
      print("\n" + Console.YELLOW + Console.BOLD + s"Running the sbt plugin for $projectPath..." + Console.RESET) 
      
      val timedExecutionResult = injectAndTest(project)
      println(timedExecutionResult.result match {
        case Okay    => "finished okay"
        case Failure => "failed"
      })
      
      Result(project, timedExecutionResult.result, timedExecutionResult.elapsed)
      
    } else {
      
      Result(project, Skipped, 0)
      
    }    
  } 
  
  Summary(results) 
  
  /*
   * runs `sbt canve` over a given project, first adding the canve sbt plugin to the project's sbt setup
   * for that sake.  
   */
  private def injectAndTest(project: Project) = {

    /*
     * add the plugin to the project's sbt setup
     */
    
    val sbtProjectDir = project.dirObj.toString + File.separator + "project"
    
    scala.tools.nsc.io.File(ReadyOutFile(sbtProjectDir, "canve.sbt"))
      .writeAll("""addSbtPlugin("canve" % "sbt-plugin" % "0.0.1")""" + "\n")      
     
    /*
     *  run sbt for the project and check for success exit code
     */
    
    val outStream = new FilteringOutputWriter(RedirectionMapper(project), (new java.util.Date).toString)
    
    val result = TimedExecution {
      Process(Seq("sbt", "-Dsbt.log.noformat=true", "canve"), project.dirObj) ! outStream == 0 match {
        case true  => Okay
        case false => Failure
      }
    }; outStream.close
    
    result
  }
}
