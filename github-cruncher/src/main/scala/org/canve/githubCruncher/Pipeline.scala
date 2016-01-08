package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

object Pipeline extends ImplicitPersistenceSerializations {
  
  object PipelineImpl extends Pipeline {
    override def rootOutputUrl = {
        new File("out").toURI // AIP outputs directory
    }
  }

  case class GenerateProjectsList() 
    extends Producer[List[play.api.libs.json.JsValue]] 
    with Ai2StepInfo with GithubCrawler {
    
      override def create = Await.result(getProjectsList, Duration(1, DAYS))
  }
 
  case class Clone() 
    extends Producer[String] 
    with Ai2StepInfo with GithubCrawler {
    
      override def create = "aaa"
  }
  
  PipelineImpl.Persist.Collection.asJson(GenerateProjectsList())
  
  def run() = {
    PipelineImpl.run("github processing pipeline")
    //PipelineImpl.openDiagram()
  }
}
