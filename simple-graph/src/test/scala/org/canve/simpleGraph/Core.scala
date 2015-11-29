import org.canve.simpleGraph._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}

class Core {
  
  case class Relation(kind: String)
  
  val graph = new SimpleGraph[Int, Unit, Relation]
  
  val GraphNode3 = graph.Vertex(3, Unit)
  
  graph ++ GraphNode3
  assert(Try(graph ++ graph.Vertex(3, Unit)).isFailure)
  assert(Try(graph ++ GraphNode3).isFailure) // we can't follow this semantic for edges though
  assert(graph.vertex(3).get == GraphNode3)
  
  assert(graph.vertex(4).isEmpty)
  val GraphNode4 = graph.Vertex(4, Unit)
  graph ++ GraphNode4
  
  assert(graph.vertex(4).get == GraphNode4)    
  assert(graph.vertex(4).get == GraphNode4)    
  assert(graph.vertex(4).get != GraphNode3)
  
  graph ++ graph.Vertex(5, Unit)
  
  val relationA = graph.Edge(3, 4, Relation("relates to"))
  val relationB = graph.Edge(3, 5, Relation("relates to"))
  
  graph ++ relationA
  graph ++ relationB
  assert(graph.vertexEdges(3).size == 2)
  
  //assert(Try(graph += relationA.copy()).isFailure)
  assert(graph.vertexEdges(3).size == 2)
  
  val relationC = graph.Edge(5, 3, Relation("relates back to"))
  graph ++ relationC
  //assert(Try(graph += relationC).isFailure)
  assert(graph.vertexEdges(3).size == 3)
  
  graph ++ graph.Edge(3, 5, Relation("relates back to"))
  assert(graph.vertexEdges(3).size == 4)

  graph ++ graph.Edge(5, 5, Relation("relates to")) // verifies relation can point to itself
  
  assert(Try(graph ++ graph.Edge(6, 5, Relation("relates back to"))).isFailure)
  
  assert(graph.vertexIterator ne graph.vertexIterator) // verifies vertexIterator returns new iterator on each call
  
  assert(graph.vertexIterator.size > 0)
  assert(graph.vertexIterator.size > 0)
 
}