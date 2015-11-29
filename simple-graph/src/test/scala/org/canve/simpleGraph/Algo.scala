import org.canve.simpleGraph._
import org.canve.simpleGraph.algo._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}
import org.canve.simpleGraph.algo.impl.GetPathsBetween

class Algo {

  object Relation 
                  
  val graph = new SimpleGraph[Int, Unit, Unit] with GetPathsBetween[Int, Unit, Unit]

  graph ++ (graph.Vertex(1, Unit), graph.Vertex(2, Unit), graph.Vertex(3, Unit), graph.Vertex(4, Unit)) 
  graph ++ (graph.Edge(1, 2, Unit), graph.Edge(2, 3, Unit), graph.Edge(3,1,Unit), graph.Edge(1,4,Unit), graph.Edge(1,1,Unit), graph.Edge(2,2,Unit), graph.Edge(3,3,Unit))
  
  def voidFilter: graph.WalkStepFilter[Int, graph.Edge] = (v, e) => true
  
  def egressFilter: graph.WalkStepFilter[Int, graph.Edge] = (v, e) => graph.direction(v, e) == Egress
  
  def testEqual(resultPaths: Option[List[List[Int]]], expected: Option[List[List[Int]]]) {
    println(resultPaths)
    assert(resultPaths == expected)
  }
  
  testEqual(graph.getPathsBetween(1, 1, egressFilter), Some(List(List(1, 2, 3, 1)))) 
  testEqual(graph.getPathsBetween(1, 1, egressFilter), Some(List(List(1, 2, 3, 1)))) // test result is consistent
  testEqual(graph.getPathsBetween(1, 1, voidFilter), Some(List(List(1, 1), List(1, 2, 1), List(1, 4, 1), List(1, 3, 1), List(1, 2, 3, 1))))
  testEqual(graph.getPathsBetween(1, 3, voidFilter), Some(List(List(1, 3), List(1, 2, 3))))
}