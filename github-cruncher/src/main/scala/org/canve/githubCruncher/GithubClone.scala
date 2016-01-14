package org.canve.githubCruncher
import org.canve.shared.Execution._
import java.io.File
import org.canve.shared._
import scala.reflect.io.Directory

trait GithubClone {
  
  //val cloneOutputPath = outDirectory + File.separator + "clone-logs" // directory to write stdout & stderr into 
  //val clonesPath = outDirectory + File.separator + "clones"          // directory to clone under
  
  def go(cloneUrl: String, projectFullName: String): String = {
    
    val out = new DataWithLog(outDirectory + File.separator + "clones" + File.separator + projectFullName)
    
    println(s"about to clone $projectFullName")  
    
    /*
     * Note: git clone writes its normal output to stdout
     * Note: need to add "--progress", as per https://git-scm.com/docs/git-clone,
     *       if you want the logging to contain the typical progress messages that 
     *       you see running git in a terminal  
     */
    
    val outcome = 
      new ProcessRun(
        outputRedirectionDir  = out.logDir.toString, 
        outputRedirectionFile = projectFullName,
        workingDirectoryAsString = out.dataDir.toString, 
        command = Seq("git", "clone", cloneUrl),
        errorLift = true).run

    out.dataDir.toString
  }
}