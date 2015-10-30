package org.canve.compilerPlugin
import scala.tools.nsc.Global
import Logging.Utility._

/*
 * TODO: turn this comment into an actionable such as logging the flags for forensics,
 *       a ticket, documentation, or whatever. 
 * 
 * Note: it is documented in the source, that 
 * a flag settings.Yshowsymowners, adds the symbol owner's id to the nameString.
 * In case this is so, owner chains may be obtained more cheaply. Also note that
 * some settings may skew the results of this code.
 */

object TraversalExtractionWriter {
  def apply(global: Global)(unit: global.CompilationUnit)(projectName: String) = {
    val graph: Graph = TraversalExtraction(global)(unit.body)

    Log(graph.nodes.size + " entities so far in project " + projectName)
    Log(graph.edges.size + " edges so far for project " + projectName)
    Output.write
    Log("done examining source file" + unit.source.path + "...")

    Unit // Should return Unit    
  }
}

object TraversalExtraction {

  def apply(global: Global)(body: global.Tree) : Graph = {
    import global._ // for having access to typed symbol methods

    /*
     * Captures the node's hierarchy chain -  
     * this is needed for the case that the node is a library symbol, 
     * so we won't (necessarily) bump into its parents while compiling
     * the project being compiled.  
     */
    def recordOwnerChain(node: Node, symbol: Symbol): Unit = {
      
      def recordOwnerChainImpl(node: Node, symbol: Symbol): Unit = {
        // Note: there is also the reflection library supplied Node.ownerChain method,
        //       for now, the recursive iteration used here instead seems as good.
        if (!node.ownersTraversed) {
          if (symbol.nameString != "<root>") {
            val ownerSymbol = symbol.owner
            val ownerNode = Nodes(global)(ownerSymbol)
            Edges(symbol.owner.id, "declares member", symbol.id)
            recordOwnerChainImpl(ownerNode, ownerSymbol)
            node.ownersTraversed = true
          }
        }
      }
      
      recordOwnerChainImpl(node, symbol)
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

                if (defParent.isDefined) Edges(defParent.get.id, "uses", select.symbol.id)
               
                val node = Nodes(global)(select.symbol)

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

                if (defParent.isDefined) Edges(defParent.get.id, "uses", select.symbol.id)

                val node = Nodes(global)(select.symbol)

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

            Nodes(global)(symbol)
            Edges(defParent.get.id, "declares member", symbol.id)

            // Capturing the defined val's type (not kind) while at it
            val valueType = symbol.tpe.typeSymbol // the type that this val instantiates.
            val node = Nodes(global)(valueType)
            recordOwnerChain(node, valueType)

            Edges(symbol.id, "is of type", valueType.id)

          // Capture defs of methods.
          // Note this will also capture default constructors synthesized by the compiler
          // and synthetic accessor methods defined by the compiler for vals
          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val symbol = tree.symbol

            Nodes(global)(symbol)
            Edges(defParent.get.id, "declares member", symbol.id)

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

            val node = Nodes(global)(typeSymbol)
            recordOwnerChain(node, typeSymbol)

            val parentTypeSymbols = parents.map(parent => parent.tpe.typeSymbol).toSet
            parentTypeSymbols.foreach { s =>
              val parentNode = Nodes(global)(s)
              recordOwnerChain(parentNode, s)
            }

            // This has actually been seen in the console one time, so keep it
            if (defParent.isDefined)
              if (defParent.get.id != typeSymbol.owner.id)
                Warning.logParentNotOwner(global)(defParent.get, typeSymbol.owner)
                
            parentTypeSymbols.foreach(s =>
              Edges(typeSymbol.id, "extends", s.id))

            val traverser = new ExtractionTraversal(Some(tree.tpe.typeSymbol))
            body foreach { tree => traverser.traverse(tree) }

          case tree =>
            super.traverse(tree)

        }
      }
    }

    val traverser = new ExtractionTraversal(None)
    traverser.traverse(body)

    performance.Counters.report((report: String) => Log(report))
    Graph(Nodes.map.map(_._2).toSet, Edges.set)    
  }
}
