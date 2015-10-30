import org.canve.simpleGraph._
import org.canve.simpleGraph.algo._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}
import org.canve.simpleGraph.algo.impl.GetPathsBetween

class Algo {
  case class Node (id: Int) extends AbstractVertex[Int]
                    
  case class Relation (id1: Int, id2: Int) extends AbstractEdge[Int]
                  
  val graph = new SimpleGraph [Int, Node, Relation]

  graph += (Node(1), Node(2), Node(3), Node(4)) 
  graph += (Relation(1, 2), Relation(2, 3), Relation(3,1), Relation(1,4), Relation(1,1), Relation(2,2), Relation(3,3))
  
  def voidFilter(filterFunc: FilterFuncArguments[Node, Relation]): Boolean = true
  def walkFilter(filterFunc: FilterFuncArguments[Node, Relation]): Boolean = {
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