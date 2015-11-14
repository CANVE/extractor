package org.canve.compilerPlugin
import scala.tools.nsc.Global
import Logging.Utility._

object TraversalExtractionWriter {
  def apply(global: Global)(unit: global.CompilationUnit)(projectName: String, graph: ExtractedGraph) = {
    
    TraversalExtraction(global)(unit.body)(graph)
    
    Log(graph.extractedSymbols.size + " symbols so far extracted for project " + projectName)
    Log(graph.extractedSymbolRelations.size + " symbol relations so far extracted for project " + projectName)
    
    Output.write(graph)

    Unit    
  }
}

object TraversalExtraction {

  def apply(global: Global)(body: global.Tree)(graph: ExtractedGraph) : ExtractedGraph = {
    import global._ // for having access to typed symbol methods

    /*
     * Captures the node's hierarchy chain -  
     * this is needed for the case that the node is a library symbol, 
     * so we won't (necessarily) bump into its parents while compiling
     * the project being compiled. And also for ultimately merging symbols
     * from different projects
     */
    def recordOwnerChain(node: ExtractedSymbol, symbol: Symbol): Unit = {
      
      def recordOwnerChainImpl(node: ExtractedSymbol, symbol: Symbol): Unit = {
        // Note: there is also the reflection library supplied Node.ownerChain method,
        //       for now, the recursive iteration used here instead seems as good.
        if (!node.ownersTraversed) {
          if (symbol.nameString != "<root>") {
            val ownerSymbol = symbol.owner
            val ownerNode = graph.extractedSymbols(global)(ownerSymbol)
            graph.extractedSymbolRelations(symbol.owner.id, "declares member", symbol.id)
            recordOwnerChainImpl(ownerNode, ownerSymbol)
            node.ownersTraversed = true 
          }
        }
      }
      
      recordOwnerChainImpl(node, symbol)
    }

    class ExtractionTraversal(defParent: Option[global.Symbol]) extends Traverser {
      override def traverse(tree: Tree): Unit = {

        // see http://www.scala-lang.org/api/2.11.0/scala-reflect/index.html#scala.reflect.api.Trees 
        // for the different cases, as well as the source of the types matched against
        tree match {

          // capture member usage
          case select: Select =>
            select.symbol.kindString match {
              case "method" | "constructor" =>
                if (defParent.isEmpty) Warning.logMemberParentLacking(global)(select.symbol)

                if (defParent.isDefined) graph.extractedSymbolRelations(defParent.get.id, "uses", select.symbol.id)
               
                val node =  graph.extractedSymbols(global)(select.symbol)

                // record the source code location where the symbol is being used by the user 
                // this is a proof of concept, that only doesn't propagate its information
                // to the UI in any way yet.
                if (defParent.isDefined) {
                  val callingSymbol = defParent.get
                  callingSymbol.sourceFile match {
                    case null => 
                    case _ =>
                      // the source code location of the call made by the caller
                      val source = callingSymbol.sourceFile.toString
                      val line = select.pos.line
                      val column = select.pos.column
                      //Log("symbol " + select.symbol.nameString + "is being used in " + source + " " + line + "," + column)
                  }
                }

                recordOwnerChain(node, select.symbol)

              case _ =>

                //Log("Processing select of kind " + select.symbol.kindString + " symbol: " + showRaw(select))

                if (defParent.isDefined) graph.extractedSymbolRelations(defParent.get.id, "uses", select.symbol.id)

                val node = graph.extractedSymbols(global)(select.symbol)

                recordOwnerChain(node, select.symbol)
            }

          /*
           *    See:
           *    https://groups.google.com/d/topic/scala-internals/Ms9WUAtokLo/discussion
           *    https://groups.google.com/forum/#!topic/scala-internals/noaEpUb6uL4
           */
          case ident: Ident => Log("ignoring Ident: " + ident.symbol)

          // Capture val definitions (rather than their automatic accessor methods..)
          case ValDef(mods: Modifiers, name: TermName, tpt: Tree, rhs: Tree) =>

            val symbol = tree.symbol

            graph.extractedSymbols(global)(symbol)
            graph.extractedSymbolRelations(defParent.get.id, "declares member", symbol.id)

            // Capturing the defined val's type (not kind) while at it
            val valueType = symbol.tpe.typeSymbol // the type that this val instantiates.
            val node = graph.extractedSymbols(global)(valueType)
            recordOwnerChain(node, valueType)

            graph.extractedSymbolRelations(symbol.id, "is of type", valueType.id)

          // Capture defs of methods.
          // Note this will also capture default constructors synthesized by the compiler
          // and synthetic accessor methods defined by the compiler for vals
          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val symbol = tree.symbol

            graph.extractedSymbols(global)(symbol)
            graph.extractedSymbolRelations(defParent.get.id, "declares member", symbol.id)

            val traverser = new ExtractionTraversal(Some(tree.symbol))
            if (symbol.nameString == "get") {
              //val tracer = new TraceTree
              //tracer.traverse(tree)
              //Log(Console.RED + Console.BOLD + showRaw(rhs))
              //Log(symbol.tpe.typeSymbol)
            }
            traverser.traverse(rhs)

          // Capture type definitions (classes, traits, objects)
          case Template(parents, self, body) =>

            val typeSymbol = tree.tpe.typeSymbol

            val node = graph.extractedSymbols(global)(typeSymbol)
            recordOwnerChain(node, typeSymbol)

            val parentTypeSymbols = parents.map(parent => parent.tpe.typeSymbol).toSet
            parentTypeSymbols.foreach { s =>
              val parentNode = graph.extractedSymbols(global)(s)
              recordOwnerChain(parentNode, s)
            }

            // This has actually been seen in the console one time, so keep it
            if (defParent.isDefined)
              if (defParent.get.id != typeSymbol.owner.id)
                Warning.logParentNotOwner(global)(defParent.get, typeSymbol.owner)
                
            parentTypeSymbols.foreach(s =>
              graph.extractedSymbolRelations(typeSymbol.id, "extends", s.id))

            val traverser = new ExtractionTraversal(Some(tree.tpe.typeSymbol))
            body foreach { tree => traverser.traverse(tree) }

          case tree =>
            super.traverse(tree)

        }
      }
    }

    // Exploration function to trace a tree
    class TraceTree extends Traverser {
      override def traverse(tree: Tree): Unit = {
        tree match {
          case _ =>
            Log(Console.GREEN + Console.BOLD + tree.getClass.getSimpleName + " " + Option(tree.symbol).fold("")(_.kindString) + " " + tree.id)
            if (tree.isType) Log("type " + tree.symbol + " (" + tree.symbol.id + ")")
            if (tree.isTerm) Log("term " + tree.symbol + " " + Option(tree.symbol).fold("")(_.id.toString))
            Log(Console.RESET)
            super.traverse(tree)
        }
      }
    }
    
    val traverser = new ExtractionTraversal(None)
    traverser.traverse(body)

    performance.Counters.report((report: String) => Log(report)) // TODO: fix it
    
    graph  
  }
}