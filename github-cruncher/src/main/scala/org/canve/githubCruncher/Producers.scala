/*
 * All producers used in this project ― their implementations are called from here
 */

package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import scala.concurrent.duration._
import scala.concurrent.Await
import org.canve.shared.Sbt.ApplyPlugin
import org.canve.shared.Sbt.Project
import java.io.File

/* Project list producer */
case class GithubProjectList() extends Producer[List[play.api.libs.json.JsValue]] 
  with Ai2StepInfo with GithubQuery {
    override def create = Await.result(go, Duration(10, MINUTES))
}

/* Project clone producer */
case class Clone(cloneUrl: String) extends Producer[String]
  with Ai2StepInfo with GithubClone {
    def create = go(cloneUrl)      
}  

/* Cloned project canve producer */
case class IsSBT(clonedUrl: String) 
  extends Producer[Boolean] with Ai2StepInfo  {
    def create = ApplyPlugin.canApply(Project(new File(clonedUrl)))
}  

/* Cloned project canve producer */
case class Canve(clonedUrl: String) 
  extends Producer[String] with Ai2StepInfo  {
    def create = ApplyPlugin(Project(new File(clonedUrl))).toString      
}  


