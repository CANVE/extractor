package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global

object PipelineDef extends ImplicitPersistenceSerializations {
  
  object Pipeline extends Pipeline {
    override def rootOutputUrl = outDirectory.toURI // AIP outputs/cache directory
  }
  
  /* 
   * adds a clone step to the pipeline per github project 
   */
  def AddProjectsToPipeline(githubQuery: Producer[Iterable[play.api.libs.json.JsValue]]) { 
    
    githubQuery.get.foreach { projectDescription =>  
      val cloneUrl: String = (projectDescription \ "clone_url").as[String]
      Pipeline.Persist.Singleton.asText(ProjectProcessor(cloneUrl))
    }
  }
  
  val projectList = Pipeline.Persist.Collection.asJson(GithubProjectList())
  AddProjectsToPipeline(projectList)
  
  def run() = {
    
    Pipeline.run("github processing pipeline")
    //PipelineImpl.openDiagram()
    
  }
}
