package org.canve.githubCruncher
import org.canve.shared.Execution._
import java.io.File
import org.canve.shared._
import scala.reflect.io.Directory

trait GithubClone {
  
  //val cloneOutputPath = outDirectory + File.separator + "clone-logs" // directory to write stdout & stderr into 
  //val clonesPath = outDirectory + File.separator + "clones"          // directory to clone under
  
  def go(cloneUrl: String, escapedFullName: String): String = {
    
    val out = new DataWithLog(outDirectory + File.separator + "clones" + File.separator + escapedFullName)
    
    /*
     * Note: git clone writes its normal output to stdout
     * Note: can add "--progress", as per https://git-scm.com/docs/git-clone,
     *       if you want the logging to contain the typical progress messages that 
     *       you see running git in a terminal  
     */
    
    val outcome = 
      new ProcessRun(
        outputRedirectionDir  = out.logDir.toString, 
        outputRedirectionFile = escapedFullName,
        workingDirectoryAsString = out.dataDir.toString, 
        command = Seq("git", "clone", cloneUrl, "."),
        errorLift = true).run

    val projectName = escapedFullName.split('.').last // only the repo name, without the owner prefix
    out.dataDir.toString 
  }
}