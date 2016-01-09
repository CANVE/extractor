package org.canve.githubCruncher
import mysql.DB

object app extends App with GithubQuery {
  
  val db = DB
  
  PipelineWrapper.run
  
}