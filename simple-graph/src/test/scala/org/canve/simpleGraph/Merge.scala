import org.canve.simpleGraph._
import utest._
import utest.ExecutionContext.RunNow
import scala.util.{Try, Success, Failure}

trait TestGraph { self: MergeTest =>
  
  trait UnitRelation; object UnitRelation extends UnitRelation
  
  val graph = new SimpleGraph[Int, Unit, UnitRelation]
  
  /* using edge data from real failed run */
  private val edgesData = Seq((14111, 325247), (325247,325246), (325246,325247), (325247,316377), (325247,325250))
  
  private val edges = edgesData.map(d => graph.Edge(d._1, d._2, UnitRelation))
  private val vertices = edgesData.flatMap(pair => Seq(pair._1, pair._2)).distinct.map(id => graph.Vertex(id, Unit))

  graph ++ vertices
  graph ++ edges   

}

class MergeTest extends TestGraph {
  
  val id1 = 325246
  val id2 = 325247
  
  assert(graph.edgesBetween(id2,id1).map(e => (e.v1, e.v2)) == Set((325247,325246), (325246,325247)))
  assert(graph.edgesBetween(id2,id1) == graph.edgesBetween(id1,id2)) 
  
  graph -= graph edgesBetween(id1,id2)
  assert(graph.edgesBetween(id2,id1).isEmpty)
  assert(graph.edgesBetween(id1,id2) == graph.edgesBetween(id2,id1))
  assert(graph.edgeIterator.map(e => (e.v1, e.v2)).toSet == Set((14111, 325247), (325247,316377), (325247,325250)))
  
  graph.vertexEdges(id2) foreach { e => 
    val size = graph.edgeIterator.size
    graph.edgeReWire(e, from = id2, to = id1)
    assert(graph.edgeIterator.size == size)
  }
  graph.edgeIterator.filter(e => e.v1 == id2 || e.v2 == id2).isEmpty
  assert(graph.edgeIterator.toList == graph.edgeIterator.toList.distinct)
  assert(graph.edgeIterator.map(e => (e.v1, e.v2)).toSet equals Set((14111, 325246), (325246,316377), (325246,325250)))
    
  graph -- id2
  
  assert(graph.vertex(id2).isEmpty)
  assert(graph.vertex(id1).nonEmpty)
  
  assert(graph.edgeIterator.map(e => (e.v1, e.v2)).toSet equals Set((14111, 325246), (325246,316377), (325246,325250)))
}