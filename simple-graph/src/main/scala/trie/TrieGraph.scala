package trie
import scala.collection.concurrent.TrieMap
import scala.util.Try

/*
 * trait to be mixed in by user code for making their nodes graph friendly
 */
trait AbstractVertex[ID] {  
  val id: ID  
} 

/*
 * trait to be mixed in by user code for making their nodes graph friendly
*/
trait AbstractEdge[ID] {
  val id1: ID
  val id2: ID
}

abstract class EdgeDirection
case object Ingress extends EdgeDirection
case object Egress extends EdgeDirection 

case class SimpleGraphException(errorText: String) extends Exception

class Graph[ID, Vertex <: AbstractVertex[ID], Edge <: AbstractEdge[ID]] {
    
  private val nodeIndex = new TrieMap[ID, Vertex]   
  private val edgeIndex = new TrieMap[ID, Set[Edge]]
  
  def addNode(vertex: Vertex): Graph[ID, Vertex, Edge] = {    
    nodeIndex.putIfAbsent(vertex.id, vertex) match {      
      case Some(vertex) => throw SimpleGraphException("node with id $id already exists in the graph")
      case None => 
    }
    this
  }
  
  def getNode(id: ID): Option[Vertex] = nodeIndex.get(id)
  
  def addEdge(edge: Edge): Graph[ID, Vertex, Edge] = this.synchronized {
    edgeIndex.get(edge.id1) match {
      case Some(set) => edgeIndex.put(edge.id1, set + edge)
      case None      => edgeIndex.put(edge.id1, Set(edge))
    }
    this
  }
  
  def getNodeEdges(id: ID): Option[Set[Edge]] = edgeIndex.get(id)
}

object Test {
  
  case class Node(id: Int, 
                  name: String,
                  kind: String) extends AbstractVertex[Int]
                  
  case class Relation(id1: Int,
                      Kind: String,
                      id2: Int) extends AbstractEdge[Int]
                  
  val graph = new Graph[Int, Node, Relation]
  
  val Node3 = Node(3, "foo3", "bar")
  
  graph.addNode(Node3)
  assert(Try(graph.addNode(Node(3, "error", "error"))).isFailure)
  assert(Try(graph.addNode(Node3)).isFailure) // we can't follow this semantic for edges, 
                                              // but it may help avoiding user code bugs
  assert(graph.getNode(3).get == Node3)
  
  assert(graph.getNode(4).isEmpty)
  val Node4 = Node(4, "foo4", "bar")
  graph.addNode(Node4)
  
  assert(graph.getNode(4).get == Node4)    
  assert(graph.getNode(4).get == Node4)    
  assert(graph.getNode(4).get != Node3)
  
  val Node5 = Node(5, "foo5", "bar")
  graph.addNode(Node5)
  
  val relationA = Relation(3, "relates to", 4)
  val relationB = Relation(3, "relates to", 5)
  
  graph.addEdge(relationA)
  graph.addEdge(relationB)
  assert(graph.getNodeEdges(3).getOrElse(Set()).size == 2)
  graph.addEdge(relationA.copy())
  assert(graph.getNodeEdges(3).getOrElse(Set()).size == 2)
  
  println(graph)

  val relationC = Relation(5, "relates back to", 3)
}