import org.canve.simpleGraph._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}

class Core {
  
  case class Node(
    id: Int, 
    name: String,
    kind: String
  ) extends AbstractVertex[Int]
                  
  case class Relation(
    id1: Int,
    Kind: String,
    id2: Int
  ) extends AbstractEdge[Int]
                  
  val graph = new SimpleGraph[Int, Node, Relation]
  
  val Node3 = Node(3, "foo3", "bar")
  
  graph.+=(Node3)
  assert(Try(graph.+=(Node(3, "error", "error"))).isFailure)
  assert(Try(graph.+=(Node3)).isFailure) // we can't follow this semantic for edges though
  assert(graph.vertex(3).get == Node3)
  
  assert(graph.vertex(4).isEmpty)
  val Node4 = Node(4, "foo4", "bar")
  graph.+=(Node4)
  
  assert(graph.vertex(4).get == Node4)    
  assert(graph.vertex(4).get == Node4)    
  assert(graph.vertex(4).get != Node3)
  
  val Node5 = Node(5, "foo5", "bar")
  graph.+=(Node5)
  
  val relationA = Relation(3, "relates to", 4)
  val relationB = Relation(3, "relates to", 5)
  
  graph.+=(relationA)
  graph.+=(relationB)
  assert(graph.vertexEdges(3).size == 2)
  
  assert(Try(graph.+=(relationA.copy())).isFailure)
  assert(graph.vertexEdges(3).size == 2)
  
  val relationC = Relation(5, "relates back to", 3)
  graph.+=(relationC)
  assert(Try(graph.+=(relationC)).isFailure)
  assert(graph.vertexEdges(3).size == 3)
  
  graph.+=(Relation(3, "relates back to", 5))
  assert(graph.vertexEdges(3).size == 4)

  graph += Relation(5, "relates to", 5) // verifies relation can point to itself
  
  assert(Try(graph.+=(Relation(6, "relates back to", 5))).isFailure)
  
  assert(graph.vertexIterator ne graph.vertexIterator) // verifies vertexIterator returns new iterator on each call 
 
}