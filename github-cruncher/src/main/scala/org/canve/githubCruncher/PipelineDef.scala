package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global

/* 
 * adds a clone step to a pipeline per github project 
 */
object AddProjects {
  def apply(pipeline: Pipeline, githubQuery: Producer[Iterable[play.api.libs.json.JsValue]]) { 
    
    githubQuery.get.foreach { projectDescription =>  
      val cloneUrl: String = (projectDescription \ "clone_url").as[String]
      pipeline.Persist.Singleton.asText(ProjectProcessor(cloneUrl))
    }
  }
}

object PipelineDef extends ImplicitPersistenceSerializations {
  
  /*
   * setup and expose a pipeline runner
   */
  
  private object Pipeline extends Pipeline {
    override def rootOutputUrl = outDirectory.toURI // directory for AIP data outputs/cache
  }
  
  private val projectList = Pipeline.Persist.Collection.asJson(GithubProjectList())
  AddProjects(Pipeline, projectList)
  
  def run = {
    Pipeline.run(title = "github processing pipeline")
    //PipelineImpl.openDiagram()
  }
}
