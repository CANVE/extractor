package org.canve.compilerPlugin
import scala.tools.nsc.Global
import org.canve.compilerPlugin.Utility._

object TraversalExtractionWriter {
  def apply(global: Global)(unit: global.CompilationUnit, projectName: String, extractedModel: ExtractedModel): ExtractedModel = {
    
    TraversalExtraction(global)(unit.body)(extractedModel)
    
    extractedModel
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
                
                if (defParent.isDefined) {
                  extractedModel.add(defParent.get.id, "uses", select.symbol.id)
                  println
                  println(s"Method call. Symbol owner chain: ${select.symbol.ownerChain.reverse}, \nParams: ${select.symbol.paramss}")
                  println("signatureString: " + select.symbol.signatureString)
                  println
                }
                
                // record the source code location where the symbol is being used by the user 
                // this is a proof of concept, that only writes to the log for now.
                // needs to happen not just for method calls but also for object usage and whatever else -
                // so that fully linked source code display has its data
                if (defParent.isDefined) {
                  val callingSymbol = defParent.get
                  callingSymbol.sourceFile match {
                    case null => 
                    case _ =>
                      // the source code location of the call made by the caller
                      val source = callingSymbol.sourceFile.toString
                      val line = select.pos.line
                      val column = select.pos.column
                      Log(s"""symbol ${select.symbol.nameString} is being used by $callingSymbol in $source ($line, $column):"
                          |$source""".stripMargin) 
                  }
                }

              case _ =>

                //Log("Processing select of kind " + select.symbol.kindString + " symbol: " + showRaw(select))

                extractedModel.add(global)(select.symbol)
                
                if (defParent.isDefined) extractedModel.add(defParent.get.id, "uses", select.symbol.id)

            }

          /*
           *    We should probably treat `Ident`s same as `Select`s, as it is possible
           *    we are missing on some symbols without that. See:
           *    https://groups.google.com/d/topic/scala-internals/Ms9WUAtokLo/discussion
           *    https://groups.google.com/forum/#!topic/scala-internals/noaEpUb6uL4
           */
          case ident: Ident => Log("ignoring Ident: " + ident.symbol)

          // Capture val definitions (rather than their automatic accessor methods..)
          case ValDef(mods: Modifiers, name: TermName, tpt: Tree, rhs: Tree) =>

            val symbol = tree.symbol
            
            extractedModel.add(global)(symbol)
            if (defParent.get.id != symbol.owner.id) println("parent is not owner")
            if (symbol.owner.id == 325220) println("found!")
            if (defParent.get.id == 325220) println("found as parent!")

            extractedModel.addIfUnique(defParent.get.id, "declares member", symbol.id)

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
            if (symbol.owner.id == 325220) println("found!")
            if (defParent.get.id == 325220) println("found as parent!")
            if (defParent.get.id != symbol.owner.id) println("parent is not owner")
            extractedModel.addIfUnique(defParent.get.id, "declares member", symbol.id)
            println
            println(s"Method definition. Symbol owner chain: ${symbol.ownerChain.reverse}, \nParams: ${symbol.paramss}")
            println("signatureString: " + symbol.signatureString)
            println

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
                
            parentTypeSymbols.foreach(s => {
              extractedModel.add(typeSymbol.id, "extends", s.id)
              println
              println(s"Symbol ${typeSymbol.ownerChain.reverse}")
              println(s"Performs Type extension of ${s.ownerChain.reverse}. TypeParams: ${s.typeParams}")
              println("signatureString: " + s.signatureString)
              println("typeSignature: " + s.typeSignature)
              println
            })

            val traverser = new ExtractionTraversal(Some(tree.tpe.typeSymbol))
            body foreach { tree => traverser.traverse(tree) }

          case tree =>
            super.traverse(tree)

        }
      }
    }

    // Exploration function tempto trace a tree
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

    performance.Counter.report((report: String) => Log(report))
    
    extractedModel  
  }
}