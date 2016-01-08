package org.canve.githubCruncher
import mysql.DB

object app extends App with GithubCrawler {
  
  val db = DB
  
  Pipeline.run
  
}