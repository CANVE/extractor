package org.canve.githubCruncher
import org.canve.shared.Execution._
import java.io.File
import org.canve.shared.ReadyOutDir

trait GithubClone {
  
  val cloneOutputPath = outDirectory + File.separator + "clone-logs"
  
  def go(cloneUrl: String) = {
    
    val cloneFolder = cloneUrl.split("/").last.dropRight(4) // naive derivation of the clone-to directory
    println(s"about to clone $cloneFolder")  
    
    val outcome = 
      ProcessRunner(
        outputRedirectionDir  = cloneOutputPath, 
        outputRedirectionFile = cloneFolder,
        command = Seq("git", "clone" ,cloneUrl),
        ReadyOutDir(outDirectory + File.separator + "clones").toString).run

    println(s"result of command is ${outcome.result.getClass} ${outcome.result}")
    cloneFolder
  }
}