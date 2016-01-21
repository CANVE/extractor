package org.canve.githubCruncher.manipulation
import better.files._, Cmds._
import org.canve.shared.IO._

object Audit extends App with Implicits {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val initialOpenDescriptors = getOpenFilesCount  
  
  def projects: Iterator[String] = 
    File("github-cruncher/out/canve")                                   // base path
    .children.map(_.name)                                                           
     
  def missingDataDirectories: Iterator[File] = 
    File("github-cruncher/out/canve")                                   // base path
    .children                                                           // all its projects under it
    .filter(_.children.filter(_.name == "data").getAndDeplete.isEmpty)  // which don't have a data directory

  def missingResultFiles: Iterator[File] = 
    File("github-cruncher/out/canve")                                   // base path
    .children                                                           // all projects under it
    .map(_.children.filter(_.name == "data").getAndDeplete)                       // data directories under each 
    .flatMap(_.filter(_.children.filter(_.name == "result.json").getAndDeplete.isEmpty)) // which lack a result file in them
    
  def resultsFiles: Iterator[File] = 
    File("github-cruncher/out/canve")                                   // base path
    .children                                                           // all projects under it
    .map(_.children.filter(_.name == "data").getAndDeplete)                       // data directories under each 
    .flatMap(_.flatMap(_.children.filter(_.name == "result.json").getAndDeplete)) // result files under them
  
  def results = 
    resultsFiles map { resultFile => 
      val f = resultFile.newInputStream
      val stringContent = f.content.mkString
      val success = (Json.parse(stringContent) \ "success").as[JsValue].as[Boolean]
      f.close
      (resultFile, success)  
  }
  
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
    println(Console.BOLD + s"Project directories found: ${projects.length}" + Console.RESET)  
    printIfAny(missingDataDirectories, "Projects missing a `data` sub-directory:") 
    printIfAny(missingResultFiles, "Projects missing a result file under `data`, so they must have crashed or are still in progress:") 
    printIfAny(results.filter(_._2 == false), "Projects that yielded a failed success state:")
    println
  }
}

/* 
 * provide a couple of Option facades to iterator polling
 * we'd rather use or borrow the scala-arm approach, but 
 * this was written before thinking about it. 
 */ 
trait Implicits {
  implicit class IteratorEnrich[T](i: Iterator[T]) {
    
    /* get the next item, as an Option */
    def get: Option[T] = i.hasNext match {
      case true  => Some(i.next)
      case false => None
    }
    
    /* usable only once: get the next item, as an Option, then deplete the iterator */
    def getAndDeplete: Option[T] = {
      val result = get
      i.length // to deplete the iterator
      result
    }
  }
}