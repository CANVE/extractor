package org.canve.githubCruncher
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import org.canve.shared.Sbt._

/*
 * The identifier for a project used by this code, 
 * deriving from the full name provided by github api.
 */
object GetEscapedFullName {
  def apply(name: String) = name.replaceAll("/",".")
}

/* 
 * adds processing steps to a pipeline per github project 
 */
object AddProjects extends ImplicitConversions {
  def apply(pipeline: Pipeline, githubProjectList: Producer[Iterable[play.api.libs.json.JsValue]]) { 
    
    githubProjectList.get.foreach { githubProject =>  
      
      val githubProjectFullName = (githubProject \ "full_name").as[String]
      val cloneUrl        = (githubProject \ "clone_url").as[String]

      val projectName = GetEscapedFullName(githubProjectFullName)
      
      val clonedFolder = pipeline.Persist.Singleton.asText(
        Clone(cloneUrl, projectName), stepName = s"Clone.$projectName").get
      val isSbtProject = pipeline.Persist.Singleton.asText(
        IsSBT(clonedFolder), stepName = s"IsSbt.$projectName").get
      
      if (isSbtProject) 
        pipeline.Persist.Singleton.asText(SbtCanve(scala.reflect.io.Directory(clonedFolder), projectName), stepName = s"Canve.$projectName")
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
  
  private val githubProjectList = Pipeline.Persist.Collection.asJson(GithubProjectList())
  AddProjects(Pipeline, githubProjectList)
  
  def run = {
    Pipeline.run(title = "github processing pipeline")
    //PipelineImpl.openDiagram()
  }
}
