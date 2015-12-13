package org.canve.compilerPlugin

/*
 * Abstract trait and implementations meant as a hook mostly for debug - for only
 * processing a subset of source files of a project
 */

abstract trait sourceFilter {
  def sourceFilter(path: String): Boolean
  var matchedAtLeastOnce = false
  def assertSourceFilterMatched = 
    if (!matchedAtLeastOnce) throw new Exception("source filter failed")
}

/* no filter */
trait voidSourceFilter extends sourceFilter {
  def sourceFilter(path: String) = {
    matchedAtLeastOnce = true
    true
  }
}

/* filters by specific source name */
trait debugSourceFilter extends sourceFilter {
  def sourceFilter(path: String) = {
    val matched = path.endsWith("/pipeline/src/main/scala/org/allenai/pipeline/PipescriptParser.scala")
    if (matched) matchedAtLeastOnce = true
    matched
  }
}