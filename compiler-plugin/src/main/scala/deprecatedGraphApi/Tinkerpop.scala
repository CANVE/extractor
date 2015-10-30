/*

/*
 * Tinkerpop initial implementation. 
 * Cons: no major graph database currently supports tinkerpop 3,
 *       except IBM BlueMix which is cheap but probably not full-fledged
 *       or performant as OrientDB or others.
 */

package deprecatedGraphApi
import extractor.plugin.{Node, Edge}

import org.apache.commons.configuration.Configuration
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.Graph.Variables
import org.apache.tinkerpop.gremlin.structure.{ Transaction, T }
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.{Graph, Vertex}

/*
 * gremlin-scala inspired gremlin java api wrapper.
 * the downside with gremlin-scala is its use of macros for marshaling and its
 * rather heavy api. We don't need generic marshaling for out simple model.
 * 
 * rational why this code and the gremlin-scala internals aren't as straightforward as they could be: 
 * https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/gremlin-users/M7knBWSHVi4/B4RPkh6nBAAJ
 * basically, it's all related to Java8 api consumption in scala.
 */
trait TinkerScalaBridge { 
  this: TinkerBasedGraph  =>
  
  /*
   * this creates a list of (property name, value) as the Java8 api expects, whereas coercion to AnyRef
   * makes sure we always pass a java object as required.
   */
  def addVertex(properties: Map[String, Any]): Vertex =
    addVertex(properties.toSeq: _*)
    
  private def addVertex(properties: (String, Any)*): Vertex = {
    val params = properties.flatMap(pair ⇒ Seq(pair._1, pair._2.asInstanceOf[AnyRef]))
    this.graph.addVertex(params)
  }
}

/*
 * tinkerpop version 3 partial implementation 
 */
abstract class TinkerBasedGraph extends GraphApi[Node, Edge] with TinkerScalaBridge {
  
  protected val graph: TinkerGraph = TinkerGraph.open

  def addNode(node: Node) = {     
    addVertex(Map("id" -> node.id))    
  }  
}
*/
