package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import org.canve.shared.Sbt._

/* 
 * adds processing steps to a pipeline per github project 
 */
object AddProjects extends ImplicitConversions {
  def apply(pipeline: Pipeline, githubQuery: Producer[Iterable[play.api.libs.json.JsValue]]) { 
    
    githubQuery.get.foreach { projectDescription =>  
      
      val projectFullName = (projectDescription \ "full_name").as[String]
      val cloneUrl        = (projectDescription \ "clone_url").as[String]

      val escapedFullName = projectFullName.replaceAll("/",".") 
      
      val clonedFolder = pipeline.Persist.Singleton.asText(Clone(cloneUrl, escapedFullName), stepName = s"Clone.$escapedFullName").get
      val isSbtProject = pipeline.Persist.Singleton.asText(IsSBT(clonedFolder), stepName = s"IsSbt.$escapedFullName").get
      if (isSbtProject) pipeline.Persist.Singleton.asText(Canve(scala.reflect.io.Directory(clonedFolder), escapedFullName),  stepName = s"Canve.$escapedFullName")
    }
  }
}

object PipelineDef extends ImplicitConversions {
  
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
