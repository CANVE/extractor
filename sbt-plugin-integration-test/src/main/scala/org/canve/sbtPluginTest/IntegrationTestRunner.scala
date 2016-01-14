package org.canve.sbtPluginTest

import org.canve.shared._
import java.io.{File}
import org.canve.shared.ReadyOutFile
import org.canve.shared.Execution._
import org.canve.shared.Sbt._
import SbtOwnBuildInfo.info._
import scala.reflect.io.Directory

/*
 * Runs canve for each project included under the designated directory, 
 * by adding the canve sbt plugin to the project's sbt setup.
 * 
 * Arguments to the app may constrict execution, as commented inline.   
 */
object Runner extends App {
  
  //val testProjectsRoot = "test-projects"
  val testProjectsRoot: String = getClass.getResource("/integration-test-projects").getFile
  
  val outRoot = ReadyOutDir(baseDirectory.toString + File.separator + "out")
  
  //println(new File(".").getAbsolutePath)
  //println(IO.getSubDirectories(testProjectsRoot))
  
  val results = IO.getSubDirectories(testProjectsRoot) map { projectDir =>
        
    /*
     * if there's no main args provided execute all tests,
     * otherwise be selective according to a first main arg's value
     */
    if ((args.isEmpty) || (args.nonEmpty && projectDir.getName.startsWith(args.head))) 
    {    
      
      print("\n" + Console.YELLOW + Console.BOLD + s"Running the sbt plugin for $projectDir..." + Console.RESET) 
      
      val outDir = outRoot + File.separator + "canve" + File.separator + projectDir.getName
      
      val timedExecutionResult = ApplyPlugin(Directory(projectDir), outDir)
      println(timedExecutionResult.result match {
        case Okay    => "finished okay"
        case Failure => "failed"
      })
      
      Result(projectDir.getName, timedExecutionResult.result, timedExecutionResult.elapsed)
      
    } else {
      
      Result(projectDir.getName, Skipped, 0)
      
    }    
  } 
  
  Summary(results) 
}

case class Result(projectName: String, result: TaskResultType, elapsed: Long)
