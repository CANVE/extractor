import org.canve.simpleGraph._
import org.canve.simpleGraph.algo._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}
import org.canve.simpleGraph.algo.impl.GetPathsBetween

class Algo {

  /*
  case class Node(id: Int) // data-less node for this test class 
  case class GraphNode(data: Node)  
     extends AbstractVertex[Int] {
       val key = data.id
  } 
  */
  
  object Relation 
  //type GraphEdge = AbstractEdge[Int, Any]
                  
  val graph = new SimpleGraph[Int, Unit, Unit] with GetPathsBetween[Int, Unit, Unit]

  graph ++ (graph.Vertex(1, Unit), graph.Vertex(2, Unit), graph.Vertex(3, Unit), graph.Vertex(4, Unit)) 
  graph ++ (graph.Edge(1, 2, Unit), graph.Edge(2, 3,Unit), graph.Edge(3,1,Unit), graph.Edge(1,4,Unit), graph.Edge(1,1,Unit), graph.Edge(2,2,Unit), graph.Edge(3,3,Unit))
  
  def voidFilter: graph.FilterFunc[Int, graph.Edge] = (v, direction) => true
  def walkFilter: graph.FilterFunc[Int, graph.Edge] = (v, direction) => direction == Egress
  
  println("graph vertices number: " + graph.vertexIterator.size)
  
  val allPaths: Option[List[List[Int]]] = graph.getPathsBetween(1, 3, walkFilter)
  assert (allPaths.nonEmpty)
  println(allPaths)
  
  println
  
  val allPaths1: Option[List[List[Int]]] = graph.getPathsBetween(1, 1, walkFilter)
  assert (allPaths1.nonEmpty)
  println(allPaths1)  
}