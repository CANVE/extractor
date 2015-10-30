/*

/*
 * scala-graph partial implementation.
 * Cons: this api has less than attractive verbs for 
 *       graph traversal. It attempts to cater for any
 *       type of conceivable graph and hypergraph which
 *       gives it too many methods and operators to choose
 *       and confuse from. (Scala 2.10 not supported).
 */
package deprecatedGraphApi
import extractor.plugin.{Node, Edge}

//import scalax.collection.Graph 
import scalax.collection.mutable.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._

abstract class ScalaGraph extends GraphApi[Node, Edge] {
  
  private val graph = Graph(Node, Edge)
  
  def addNode(node: Node) {
    graph += node
  }  
}

*/