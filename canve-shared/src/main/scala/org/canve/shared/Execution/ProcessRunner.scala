package org.canve.shared.Execution

import scala.sys.process._
import org.canve.shared.ReadyOutFile

abstract class TaskResultType
object Okay    extends TaskResultType
object Failure extends TaskResultType
object Skipped extends TaskResultType

case class ProcessRunner(
  outputRedirectionPath: String, 
  outputRedirectionFile: String,
  command: Seq[String],
  workingDirectoryAsString: String) {
  
  def apply = {
  
    val outStream = new FilteringOutputWriter(
      ReadyOutFile(outputRedirectionPath, outputRedirectionFile),
      (new java.util.Date).toString)
      
    val result: TimedExecutionResult[TaskResultType] = TimedExecution {
      Process(command, new java.io.File(workingDirectoryAsString)) ! outStream == 0 match {
        case true  => Okay
        case false => Failure
      }
    }
    
    outStream.close
    
    result
  }
}