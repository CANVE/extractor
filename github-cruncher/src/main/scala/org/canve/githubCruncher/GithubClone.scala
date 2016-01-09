package org.canve.githubCruncher

trait GithubClone {
  def go(cloneUrl: String) = {
    
    cloneUrl.split("/").last.dropRight(4) // naive derivation of the clone-to directory
  }
}