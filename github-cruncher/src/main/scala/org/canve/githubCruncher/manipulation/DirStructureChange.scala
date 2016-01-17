package org.canve.githubCruncher.manipulation
import better.files._, Cmds._
import org.canve.shared.IO._

/*
 * hinging on http://stackoverflow.com/questions/34838169/iterating-files-in-scala-java-without-leaking-open-file-descriptors,
 * as nothing practical can be gotten done with the nio stream based api ― will crash over too many 
 * open files for anything beyond a single iteration. 
 */
object Manipulate {

  println(getOpenFilesCount)
  
  val clones = File("github-cruncher/out/clones").children 
  val dataDirs = clones map { dir => 
    val entries = dir.children.filter(dir => dir.name != "creation-log")
    val dataDir = entries.next
    if (entries.hasNext) throw new Exception(s"aborting: $dir has ${1 + entries.size} children")
    dataDir
  }
  
  val cloneDirs = dataDirs.map(repo => repo.children.next)
  cloneDirs.foreach(_.renameTo("../temp"))
  
  dataDirs.foreach(dir => println(dir.list))
  
  println(getOpenFilesCount)

}