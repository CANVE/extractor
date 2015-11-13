import org.canve.simpleGraph._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}

class CoreRemovals {
  
  case class Entity(id: Int, name: String, kind: String) 
  case class GraphNode(data: Entity)  
     extends AbstractVertex[Int] {
       val key = data.id
  }    

  case class Relation(kind: String)
  case class GraphEdge(id1: Int, data: Relation, id2: Int) extends AbstractEdge[Int, Relation]
  
  val graph = new SimpleGraph[Int, Relation, GraphNode, GraphEdge]
  
  val GraphNode3 = GraphNode(Entity(3, "foo3" , "bar"))
  
  graph += GraphNode3
  assert(Try(graph += GraphNode(Entity(3, "error", "error"))).isFailure)
  assert(Try(graph += GraphNode3).isFailure) // we can't follow this semantic for edges though
  assert(graph.vertex(3).get == GraphNode3)
  
  assert(graph.vertex(4).isEmpty)
  val GraphNode4 = GraphNode(Entity(4, "foo4", "bar"))
  graph += GraphNode4
  
  assert(graph.vertex(4).get == GraphNode4)    
  assert(graph.vertex(4).get == GraphNode4)    
  assert(graph.vertex(4).get != GraphNode3)
  
  graph += GraphNode(Entity(5, "foo5", "bar"))
  
  val relationA = GraphEdge(3, Relation("relates to"), 4)
  val relationB = GraphEdge(3, Relation("relates to"), 5)
  
  graph += relationA
  graph += relationB
  assert(graph.vertexEdges(3).size == 2)
  
  assert(Try(graph += relationA.copy()).isFailure)
  assert(graph.vertexEdges(3).size == 2)
  
  val relationC = GraphEdge(5, Relation("relates back to"), 3)
  graph += relationC
  assert(Try(graph += relationC).isFailure)
  assert(graph.vertexEdges(3).size == 3)
  
  graph += GraphEdge(3, Relation("relates back to"), 5)
  assert(graph.vertexEdges(3).size == 4)

  graph += GraphEdge(5, Relation("relates to"), 5) // verifies relation can point to itself
  
  assert(Try(graph += GraphEdge(6, Relation("relates back to"), 5)).isFailure)
  
  assert(graph.vertexIterator ne graph.vertexIterator) // verifies vertexIterator returns new iterator on each call 
 
}