package org.canve.compilerPlugin.test

import org.canve.compilerPlugin._
import scala.tools.nsc.Global
import utest._
import utest.ExecutionContext.RunNow
import compilerPluginUnitTest.InjectingCompilerFactory
import org.canve.simpleGraph._
import org.canve.simpleGraph.algo.impl._

/*
 * injectable that activates this compiler's core phase, and then
 * does some testing with its output
 */

object MyTestSuite extends TestSuite {
  
  val compiler = InjectingCompilerFactory(PluginCoreRunner)
 
  val tests = TestSuite {
    
    "will title this later" - { 
      
      "case class, with new keyword" - {  
        compiler.compileCodeSnippet("""
          class A[T]
        """)
      }
    }
  }
}
  
/* Does these test still bear any relevance ? 

object MyTestSuite extends TestSuite {
  val compiler = InjectingCompilerFactory(InstantiationTester)
 
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
      }
    }
  }
  
  //val results = tests.run().toSeq.foreach(println)

  //println(results.toSeq.length) // 4
  //println(results.leaves.length) // 3
  //println(results.leaves.count(_.value.isFailure)) // 2
  //println(results.leaves.count(_.value.isSuccess)) // 1
}

*/