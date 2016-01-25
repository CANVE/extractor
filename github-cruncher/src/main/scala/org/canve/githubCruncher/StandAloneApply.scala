package org.canve.githubCruncher

import org.canve.shared.DataWithLog
import org.canve.shared.Sbt.SbtPluginApplication
import org.canve.shared.Execution.TimedExecutionReport
import java.io.{File}

/*
 * Applies the plugin to an arbitrary path
 */
object StandAloneApply extends App {
  
  val path = args.length match {
    case len: Int if len > 1  => throw new Exception("Only one argument is allowed, doing nothing.") 
    case len: Int if len == 0 => throw new Exception("No argument supplied, doing nothing. Need to supply a path to an sbt project")
    case len: Int if len == 1 =>
      args.head
  }
  
  println(s"applying the sbt plugin to ${args.head} (should be an sbt project path)")
  
  val resultsDir = new DataWithLog(path + File.separator + "canve-data") 

  val result = SbtPluginApplication(scala.reflect.io.Directory(path), resultsDir) 

  println
  println(s"sbt success return code: ${result.result}")
  println(s"elapsed time: ${result.elapsed/1000} seconds")
  println(s"outputs in file://${resultsDir.base}")
}