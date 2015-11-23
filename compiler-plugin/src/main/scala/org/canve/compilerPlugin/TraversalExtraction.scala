package org.canve.compilerPlugin
import scala.tools.nsc.Global
import Logging.Utility._

object TraversalExtractionWriter {
  def apply(global: Global)(unit: global.CompilationUnit, projectName: String): ExtractedModel = {
    
    val graph: ExtractedModel = new ExtractedModel(global)
    
    TraversalExtraction(global)(unit.body)(graph)
    
    def MergeSameSpanSymbols(extractedModel: ExtractedModel) = {
      
      def pointOrStart(location: Location): Option[Int] = location match {
        case Span(start, end) => Some(start)
        case Point(loc)       => Some(loc)
        case NoLocationInfo   => None
      }
      
      def getLocation(e: ExtractedSymbol) = graph.codes.get(e.symbolCompilerId).location
      
      /*
       * group extracted code elements by their start position or singular location property
       */
      val symbolsByQualifiedId = extractedModel.graph.vertexIterator.toList.groupBy(m => m.data.qualifiedId)
      .foreach { e => val list = e._2
        if (list.size > 2) 
          throw DataNormalizationException(s"Unexpected amount of mergeable symbols for single source location: $e")
        
        if (list.size == 2) {
          val location1 = getLocation(list.head.data)
          val location2 = getLocation(list.last.data)
          if (pointOrStart(location1) == pointOrStart(location2))
          (location1, location2) match {
            case (Span(_,_), Point(_))  => println("delete duplicate span symbol") 
            case (Point(_), Span(_,_))  => println("delete duplicate span symbol")
            case (Span(_,_), Span(_,_)) => throw DataNormalizationException(s"attempt at normalizing two spans is invalid")
            case (Point(_), Point(_))   => throw DataNormalizationException(s"attempt at normalizing two spans is invalid")
          }
        }
      }
    }
    
    MergeSameSpanSymbols(graph)
    graph
  }
}

object TraversalExtraction {

  def apply(global: Global)(body: global.Tree)(extractedModel: ExtractedModel) : ExtractedModel = {
    import global._ // for having access to typed symbol methods

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

                extractedModel.add(global)(select.symbol)
                
                if (defParent.isDefined) extractedModel.add(defParent.get.id, "uses", select.symbol.id)

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

              case _ =>

                //Log("Processing select of kind " + select.symbol.kindString + " symbol: " + showRaw(select))

                extractedModel.add(global)(select.symbol)
                
                if (defParent.isDefined) extractedModel.add(defParent.get.id, "uses", select.symbol.id)

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

            extractedModel.add(global)(symbol)
            extractedModel.add(defParent.get.id, "declares member", symbol.id)

            // Capturing the defined val's type (not kind) while at it
            val valueType = symbol.tpe.typeSymbol // the type that this val instantiates.

            extractedModel.add(global)(valueType)
            extractedModel.add(symbol.id, "is of type", valueType.id)

          // Capture defs of methods.
          // Note this will also capture default constructors synthesized by the compiler
          // and synthetic accessor methods defined by the compiler for vals
          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val symbol = tree.symbol

            extractedModel.add(global)(symbol)
            extractedModel.add(defParent.get.id, "declares member", symbol.id)

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

            extractedModel.add(global)(typeSymbol)

            val parentTypeSymbols = parents.map(parent => parent.tpe.typeSymbol).toSet
            parentTypeSymbols.foreach { s => extractedModel.add(global)(s) }

            // This has actually been seen in the console one time, so keep it
            if (defParent.isDefined)
              if (defParent.get.id != typeSymbol.owner.id)
                Warning.logParentNotOwner(global)(defParent.get, typeSymbol.owner)
                
            parentTypeSymbols.foreach(s =>
              extractedModel.add(typeSymbol.id, "extends", s.id))

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

    performance.Counters.report((report: String) => Log(report))
    
    extractedModel  
  }
}