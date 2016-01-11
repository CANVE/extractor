package org.canve.sbtPluginTest

import org.canve.shared.CanveDataIO
import java.io.{File}
import org.canve.shared.ReadyOutFile
import org.canve.shared.Execution._
import org.canve.shared.Sbt._

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
      
      val timedExecutionResult = ApplyPlugin(project)
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
}

case class Result(project: Project, result: TaskResultType, elapsed: Long)
