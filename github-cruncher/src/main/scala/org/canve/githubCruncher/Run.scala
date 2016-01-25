package org.canve.githubCruncher
import mysql.DB

object GithubPipelineRun extends App with GithubQuery {
  
  val db = DB // not used for now
  
  PipelineDef.run
  
}