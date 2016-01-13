package org.canve.githubCruncher
import org.canve.shared.Execution._
import java.io.File
import org.canve.shared.ReadyOutDir

trait GithubClone {
  
  val cloneOutputPath = outDirectory + File.separator + "clone-logs" // directory to write stdout & stderr into 
  val clonesPath = outDirectory + File.separator + "clones"          // directory to clone under
  
  def go(cloneUrl: String) = {
    
    val projectName = cloneUrl.split("/").last.dropRight(4) // rather than this derivation, can get it directly from the github query response that leads to here
    
    println(s"about to clone $projectName")  
    
    /*
     * Note: git clone writes its normal output to stdout
     * Note: need to add "--progress", as per https://git-scm.com/docs/git-clone,
     *       if you want the logging to contain the typical progress messages that 
     *       you see running git in a terminal  
     */
    
    val outcome = 
      new ProcessRun(
        outputRedirectionDir  = cloneOutputPath, 
        outputRedirectionFile = projectName,
        command = Seq("git", "clone", cloneUrl),
        ReadyOutDir(clonesPath).toString, errorLift = true).run

    clonesPath + File.separator + projectName
  }
}