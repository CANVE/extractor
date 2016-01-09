package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

object PipelineWrapper extends ImplicitPersistenceSerializations {
  
  object Pipeline extends Pipeline {
    override def rootOutputUrl = {
      new File("github-cruncher-out").toURI // AIP outputs directory
    }
  }

  case class GithubProjectList() extends Producer[List[play.api.libs.json.JsValue]] 
    with Ai2StepInfo with GithubQuery {
      override def create = Await.result(go, Duration(1, DAYS))
  }
 
  def AddProjectsToPipeline(githubQuery: Producer[List[play.api.libs.json.JsValue]]) { 
    
    /* add a clone step to the pipeline per github project */
    githubQuery.get.foreach { projectDescription =>  
      val cloneUrl: String = (projectDescription \ "clone_url").as[String]
      Pipeline.Persist.Singleton.asText(ProjectAdder(cloneUrl))
    }
    
    case class ProjectAdder(cloneUrl: String) extends Producer[String]
      with Ai2StepInfo with GithubClone {
        def create = go(cloneUrl)      
    }
  }
  
  Pipeline.Persist.Collection.asJson(GithubProjectList())
  
  def run() = {
    
    Pipeline.run("github processing pipeline")
    //PipelineImpl.openDiagram()
    
    
  }
}
