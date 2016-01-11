package org.canve.githubCruncher
import org.canve.shared.Execution._
import java.io.File
import org.canve.shared.ReadyOutDir

trait GithubClone {
  
  val cloneOutputPath = outDirectory + File.separator + "clone-logs"
  
  def go(cloneUrl: String) = {
    
    val cloneFolder = cloneUrl.split("/").last.dropRight(4) // naive derivation of the clone-to directory
    println(s"about to clone $cloneFolder")  
    
    /*
     * Note: git clone writes its normal output to stdout
     * Note: need to add "--progress", as per https://git-scm.com/docs/git-clone,
     *       if you want the logging to contain the typical progress messages that 
     *       you see running git in a terminal  
     */
    
    val outcome = 
      new ProcessRun(
        outputRedirectionDir  = cloneOutputPath, 
        outputRedirectionFile = cloneFolder,
        command = Seq("git", "clone", cloneUrl),
        ReadyOutDir(outDirectory + File.separator + "clones").toString, errorLift = true).run

    cloneFolder
  }
}