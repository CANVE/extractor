package org.canve.githubCruncher.audit
import better.files.{File => Path, _}  
import org.canve.shared.IO._
import play.api.libs.json._
import org.allenai.pipeline._
import org.allenai.pipeline.IoHelpers._
import SbtOwnBuildInfo.info._
import org.canve.shared.ReadyOutDir
import org.canve.shared.DataWithLog

object Audit extends App with IteratorAsOption with FileEnrichments {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val initialOpenDescriptors = getOpenFilesCount  
  
  def projects: Iterator[String] = 
    Path("github-cruncher/out/canve")                                   // base path
    .children.map(_.name)                                                           
     
  def missingDataDirectories: Iterator[Path] = 
    Path("github-cruncher/out/canve")                                   // base path
    .children                                                           // all its projects under it
    .filter(_.children.filter(_.name == "data").safeGetSingle.isEmpty)  // which don't have a data directory

  def printIfAny[T](iterator: Iterator[T], title: String = "", color: Boolean = true) {
    val collection = iterator.toList
    if (collection.size > 0) {
      if (color) print(Console.BLUE + Console.BOLD)
      println(title)      
      if (color) print(Console.RESET)
      
      println(collection.mkString("\n"))
    }
  }
  
  WithLeakAccounting { 

    println
    println(Console.BOLD + s"sbt project directories found: ${projects.length}" + Console.RESET)   
    printIfAny(missingDataDirectories, "Projects missing a `data` sub-directory:") 

    val projectResults: List[DataWithLog] =
      Path("github-cruncher/out/canve").children.map(_.toString).map(new DataWithLog(_)).toList
    
    val (sbtSuccess, sbtFailure) = 
      projectResults.toSet.partition { project => // assumes sbt.json exists (as we wrap around sbt fairly well)
        Path(project.dataDir.toString).getChildByName("sbt.json").collect { case file => 
          file.withContent { content => 
            (Json.parse(content.mkString) \ "completion").as[JsValue].as[String]
          }
        }.get == "normal"
      }
    
    val havingSbtPluginResultsFile = 
      projectResults.toSet.filter { project =>
        Path(project.dataDir.toString).getChildByName("sbtPlugin.json").nonEmpty
      }
        
     val (sbtPluginSuccess, sbtPluginFailure) = 
       havingSbtPluginResultsFile.partition { project => 
        Path(project.dataDir.toString).getChildByName("sbtPlugin.json").get.withContent { content => 
          (Json.parse(content.mkString) \ "completion").as[JsValue].as[String] == "normal"
        }
     }
    
    /*
     * The runner and the sbt plugin cannot strictly pass elaborate return status between them.
     * Hence we get overlapping groups to infer the state of a run from.  
     */
    println(s"sbt command: normal completion ― ${sbtSuccess.size} | abnormal termination ― ${sbtFailure.size}")    
    println(s"sbt plugin : normal completion ― ${sbtPluginSuccess.size} | orderly failed ― ${sbtPluginFailure.size}")   
    println(s"sbt failures caused by the sbt plugin: ${sbtFailure.intersect(sbtPluginFailure).size}")
    println(s"sbt failures not caused by the sbt plugin: ${sbtFailure.diff(sbtPluginFailure).size} (implying sbt could not load the project)")
    println(s"sbt plugin failures that did not fail sbt: ${sbtPluginFailure.diff(sbtFailure).size} (typically should not happen..)")
    
    println
  }
}

/*
 * Methods for idiomatic, non leaking file and directory usage
 */
trait FileEnrichments extends IteratorAsOption {
  implicit class FileEnrich(f: Path) {
    
    /* gets named child of path, assuming it is a directory */
    def getChildByName(name: String): Option[Path] = {
      require(f.isDirectory, "a file cannot be searched for children (only a directory can)")
      f.children.filter(_.name == name).safeGetSingle
    }

    /* calls given function on this path's contents, then closes the file */
    def withContent[T](func: String => T) = {
      val inputStream = f.newInputStream
      val funcResult = func(inputStream.content.mkString)
      inputStream.close
      funcResult
    }
  }    
}

/* 
 * provide a couple of Option facades to iterator polling
 */ 
trait IteratorAsOption {
  implicit class IteratorEnrich[T](i: Iterator[T]) {
    
    /* gets the next item, as an Option */
    def get: Option[T] = i.hasNext match {
      case true  => Some(i.next)
      case false => None
    }
    
    /* 
     * extracts an Option from the iterator ― 
     * use this for extracting a single result from an iterator filter 
     */
    def safeGetSingle: Option[T] = {
      val result = get
      require(!i.hasNext, "this method expects only a single value present in the iterator") 
      result
    }
  }
}



object AuditPipeline extends Pipeline {
  override def rootOutputUrl = org.canve.githubCruncher.outDirectory.toURI 
}