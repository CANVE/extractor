/*
 * All producers used in this project ― their implementations are called from here
 */

package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import scala.concurrent.duration._
import scala.concurrent.Await
import org.canve.shared.Sbt.SbtPluginApplication
import java.io.File
import scala.reflect.io.Directory
import org.canve.shared.Execution.TimedExecutionReport
import org.canve.shared.DataWithLog

/* Project list producer */
case class GithubProjectList() extends Producer[List[play.api.libs.json.JsValue]] 
  with Ai2StepInfo with GithubQuery {
    override def create = Await.result(go, Duration(10, MINUTES))
}

/* Project clone producer */
case class Clone(cloneUrl: String, projectFullName: String) extends Producer[String]
  with Ai2StepInfo with GithubClone {
    def create = go(cloneUrl, projectFullName)      
}  

/* Cloned project canve producer */
case class IsSBT(clonedUrl: String) 
  extends Producer[Boolean] with Ai2StepInfo  {
    def create = SbtPluginApplication.canApply(Directory(clonedUrl))
}  

/* Cloned project canve producer */
case class SbtCanve(projectClone: Directory, projectFullName: String) 
  extends Producer[String] with Ai2StepInfo  {
    val resultsDir = new DataWithLog(outDirectory + File.separator + "canve" + File.separator + projectFullName) 
    def create = SbtPluginApplication(projectClone, resultsDir) match {
      case TimedExecutionReport(true, elapsed)  => resultsDir.base.toString
      case TimedExecutionReport(false, elapsed) => resultsDir.base.toString
    }
}  

/* move to pipeline */
abstract class DataCreationSuccess
case class Successful[T](data: T) extends DataCreationSuccess
case class Failed[T](data: T) extends DataCreationSuccess

