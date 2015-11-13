import org.canve.simpleGraph._
import org.canve.simpleGraph.algo._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}
import org.canve.simpleGraph.algo.impl.GetPathsBetween

class Algo {
  case class Node(id: Int) // data-less node for this test class 
  case class GraphNode(data: Node)  
     extends AbstractVertex[Int] {
       val key = data.id
  }                     
  
  object Relation 
  case class GraphEdge(id1: Int, id2: Int, data: Any = Relation) extends AbstractEdge[Int, Any]
                  
  val graph = new SimpleGraph[Int, Any, GraphNode, GraphEdge]

  graph += (GraphNode(Node(1)), GraphNode(Node(2)), GraphNode(Node(3)), GraphNode(Node(4))) 
  graph += (GraphEdge(1, 2), GraphEdge(2, 3), GraphEdge(3,1), GraphEdge(1,4), GraphEdge(1,1), GraphEdge(2,2), GraphEdge(3,3))
  
  def voidFilter(filterFunc: FilterFuncArguments[GraphNode, GraphEdge]): Boolean = true
  def walkFilter(filterFunc: FilterFuncArguments[GraphNode, GraphEdge]): Boolean = {
    filterFunc.direction == Egress
  }
  
  val allPaths: Option[List[List[Int]]] = new GetPathsBetween(graph, walkFilter, 1, 3).run
  assert (allPaths.nonEmpty)
  println(allPaths)
  
  println
  
  val allPaths1: Option[List[List[Int]]] = new GetPathsBetween(graph, walkFilter, 1, 1).run
  assert (allPaths1.nonEmpty)
  println(allPaths1)  
}