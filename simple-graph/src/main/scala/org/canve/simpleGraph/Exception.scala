package org.canve.simpleGraph

/* package exceptions */

abstract class SimpleGraphException(errorText: String) extends Exception(errorText) 

case class SimpleGraphDuplicate(errorText: String) extends SimpleGraphException(errorText: String)

case class SimpleGraphInvalidEdge(errorText: String) extends SimpleGraphException(errorText: String)

case class SimpleGraphInvalidVertex(errorText: String) extends SimpleGraphException(errorText: String)

case class SimpleGraphApiException(errorText: String) extends SimpleGraphException(errorText: String)

object DataWarning {
  def apply(warning: String) {
    println(s"[canve data warning] $warning")
  }
}