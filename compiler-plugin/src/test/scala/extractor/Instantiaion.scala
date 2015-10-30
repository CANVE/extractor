package extractor.test

import org.canve.compilerPlugin.{TraversalExtraction, Graph, Node, Edge}
import scala.tools.nsc.Global
import utest._
import utest.ExecutionContext.RunNow
import compilerPluginUnitTest.InjectingCompilerFactory
import org.canve.simpleGraph._
import org.canve.simpleGraph.algo.impl._

/*
 * core injectable that activates this compiler plugin's core phase and no more
 */
class TraversalExtractionTester extends compilerPluginUnitTest.Injectable {
  def apply(global: Global)(body: global.Tree) = {
    TraversalExtraction(global)(body)    
  }   
}

/*
 * Search functions
 *  
 * TODO: consider indexing later on
 * TODO: consider moving over to the simple-graph library if this trait grows much
 */
trait NodeSearch { 
  def search(graph: NodeGraph, name: String, kind: String): List[Node] = {
    graph.vertexIterator.filter(node => node.name == name && node.kind == kind).toList
  }
  
 def findUnique(graph: SimpleGraph[Int, Node, Edge], name: String, kind: String): Option[Node] = { 
   val finds = search(graph, name, kind)
   if (finds.size == 1) 
     Some(finds.head)
   else
     None
 }
 
 def findUniqueOrThrow(graph: SimpleGraph[Int, Node, Edge], name: String, kind: String): Node = {
   findUnique(graph, name, kind) match {
     case None => throw new Exception(s"graph $graph has ${search(graph, name, kind)} search results for name=$name, kind=$kind rather than exactly one.") 
     case Some(found) => found
   }
 }
}

/*
 * injectable that activates this compiler's core phase, and then
 * does some testing with its output
 */
object InstantiationTester extends TraversalExtractionTester with NodeSearch {
  override def apply(global: Global)(body: global.Tree) = {
    
    val graph: Graph = TraversalExtraction(global)(body) // ; println(graph)
    val simpleGraph = new NodeGraph(graph.nodes, graph.edges)
    
    val origin: Node = findUniqueOrThrow(simpleGraph, "Foo", "object")
    val target: Node = findUniqueOrThrow(simpleGraph, "Bar", "class")

    val targetMethods = simpleGraph.vertexEdgePeersFiltered(
      target.id, 
      (filterFuncArgs: FilterFuncArguments[Node, Edge]) => 
          (filterFuncArgs.direction == Egress && filterFuncArgs.edge.edgeKind == "declares member")) 
    
    println(s"Tracing all acceptable paths from node id ${origin.id} to node id ${target.id}")
    println(s"origin node: $origin")
    println(s"target node: $target")
    println
    
    def voidFilter(filterFuncArgs: FilterFuncArguments[Node, Edge]): Boolean = true
    def walkFilter(filterFuncArgs: FilterFuncArguments[Node, Edge]): Boolean = {      
      (filterFuncArgs.direction == Egress && filterFuncArgs.edge.edgeKind == "declares member") ||
      (filterFuncArgs.direction == Egress && filterFuncArgs.edge.edgeKind == "uses") ||
      (filterFuncArgs.direction == Egress && filterFuncArgs.edge.edgeKind == "is instance of")  
    }
    
    val allPaths: Set[Option[List[List[Int]]]] = targetMethods.map(target => new GetPathsBetween(simpleGraph, walkFilter, origin.id, target).run)
    val paths: Option[List[List[Int]]] = Some(allPaths.flatten.flatten.toList)
     
    paths match {
      case None => throw new Exception("relationsnip not found")
      case Some(paths) => { 
        println(s"${paths.size} paths found:")
        paths.foreach { path => 
          println 
          path.map(id => println(simpleGraph.vertex(id).get))
          println(target)
        }
      }
    }
  }  
}

object MyTestSuite extends TestSuite {
  val compiler = InjectingCompilerFactory(InstantiationTester)
  
  assert(!compiler.reporter.hasErrors)
  
  val tests = TestSuite {
    
    "instantiation is reasonably captured" - { 
      
      "case class, with new keyword" - {  
        compiler.compileCodeSnippet("""
          case class Bar(a:Int) 
          object Foo {
            def get(g: Int) = {
              new Bar(g)
            }
          }
        """)
        assert(!compiler.reporter.hasErrors)
      }
      
      "case class, without new keyword" - {  
        compiler.compileCodeSnippet("""
          case class Bar(a:Int) 
          object Foo {
            def get(g: Int) = {
              Bar(g)
            }
          }
          """)
        assert(!compiler.reporter.hasErrors)
      }

      "non-case class" - {  
        compiler.compileCodeSnippet("""
          class Bar(a:Int) 
          object Foo {
            def get(g: Int) = {
              new Bar(g)
            }
          }
          """)
        assert(!compiler.reporter.hasErrors)
      }
    }
  }
  
  //val results = tests.run().toSeq.foreach(println)

  //println(results.toSeq.length) // 4
  //println(results.leaves.length) // 3
  //println(results.leaves.count(_.value.isFailure)) // 2
  //println(results.leaves.count(_.value.isSuccess)) // 1
}

