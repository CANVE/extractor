package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import scala.concurrent.duration._
import scala.concurrent.Await

/* Project list producer */
case class GithubProjectList() extends Producer[List[play.api.libs.json.JsValue]] 
  with Ai2StepInfo with GithubQuery {
    override def create = Await.result(go, Duration(1, DAYS))
}

/* Project processing producer */
case class ProjectProcessor(cloneUrl: String) extends Producer[String]
  with Ai2StepInfo with GithubClone {
    def create = go(cloneUrl)      
}  