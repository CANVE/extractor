package deprecatedGraphApi
import org.canve.compilerPlugin.{Node, Edge}

/*
 * Any usage should go through this abstract class,
 * so that the underlying graph API can be changed by will
 */
abstract class GraphApi[VertexType, EdgeType] {
      
  def addNode(node: VertexType)
  
  def addEdge(edge: EdgeType)
  
  def getNode(id: Int): VertexType 
  
  def getEdges(node1: Int, node2: Int): List[EdgeType]
      
  def getEdges(node1: Int, edgeKind: String, node2: Int): List[EdgeType] 
  
}

