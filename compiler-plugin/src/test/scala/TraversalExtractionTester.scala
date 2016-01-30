package org.canve.compilerPlugin.test

import org.canve.compilerPlugin._
import scala.tools.nsc.Global
import utest._
import utest.ExecutionContext.RunNow
import compilerPluginUnitTest.InjectingCompilerFactory
import org.canve.simpleGraph._
import org.canve.simpleGraph.algo.impl._

/*
 * Injectable that activates this compiler plugin's core phase and no more.
 * It currently *does not* trigger writing the extracted model to files, rather
 * it is only returning it to the caller.
 */
object PluginCoreRunner extends compilerPluginUnitTest.Injectable {
  def apply(global: Global)(body: global.Tree) = {
    val model: ExtractedModel = TraversalExtraction(global)(body)(new ExtractedModel(global))
  }   
}
