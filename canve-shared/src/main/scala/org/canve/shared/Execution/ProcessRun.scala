package org.canve.shared.Execution

import scala.sys.process._
import org.canve.shared.ReadyOutFile

abstract class TaskResultType
object Okay    extends TaskResultType
object Failure extends TaskResultType
object Skipped extends TaskResultType

class ProcessRun(
  outputRedirectionDir: String, 
  outputRedirectionFile: String,
  command: Seq[String],
  workingDirectoryAsString: String = "./",
  errorLift: Boolean = false) {
  
  def run = {
  
    val outStream = 
      new FilteringOutputWriter(
            ReadyOutFile(outputRedirectionDir, outputRedirectionFile),
            (new java.util.Date).toString,
            errorLift)
      
    val result: TimedExecutionReport[TaskResultType] = TimedExecution {
      Process(command, new java.io.File(workingDirectoryAsString)) ! outStream == 0 match {
        case true  => Okay
        case false => Failure
      }
    }
    
    outStream.close
    
    result
  }
}