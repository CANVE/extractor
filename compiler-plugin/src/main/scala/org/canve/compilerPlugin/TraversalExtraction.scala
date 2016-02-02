package org.canve.compilerPlugin
import scala.tools.nsc.Global
import org.canve.logging.loggers._
import org.canve.compilerPlugin.Utility._
//import TypeExtraction._

object TraversalExtractionWriter {
  def apply(global: Global)(unit: global.CompilationUnit, projectName: String, extractedModel: ExtractedModel): ExtractedModel = {
    
    TraversalExtraction(global)(unit.body)(extractedModel)
    
    extractedModel
  }
}

object TraversalExtraction {

  def apply(global: Global)(body: global.Tree)(extractedModel: ExtractedModel) : ExtractedModel = {
    import global._ // for having access to typed symbol methods

    def logParentIsOwner(symbol: global.Symbol, parent: global.Symbol) {
      if (symbol.owner.id != parent.id) println(s"parent is not owner for $symbol")  
    }
    
    class ExtractionTraversal(defParent: Option[global.Symbol]) extends Traverser {
      override def traverse(tree: Tree): Unit = {

        // see http://www.scala-lang.org/api/2.11.0/scala-reflect/index.html#scala.reflect.api.Trees 
        // for the different cases, as well as the source of the types matched against
        tree match {

          /* capture member usage */
          case select: Select => // https://youtu.be/WxyyJyB_Ssc?t=1448
            select.symbol.kindString match {
              case "method" | "constructor" =>
                if (defParent.isEmpty) Warning.logMemberParentLacking(global)(select.symbol)

                extractedModel.add(global)(select.symbol)
                
                if (defParent.isDefined) {
                  extractedModel.add(defParent.get.id, "uses", select.symbol.id)
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
                      //Log(s"""symbol ${select.symbol.nameString} is being used by $callingSymbol in $source ($line, $column):"
                      //    |$source""".stripMargin) 
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
          case ident: Ident => // Log("ignoring Ident: " + ident.symbol)

          /* Capture val definitions (rather than their automatic accessor methods..) */
          case ValDef(mods: Modifiers, name: TermName, tpt: Tree, rhs: Tree) =>

            val symbol = tree.symbol
            
            extractedModel.add(global)(symbol)
            
            logParentIsOwner(symbol, parent = defParent.get)

            extractedModel.addIfUnique(defParent.get.id, "declares member", symbol.id)

            // Capturing the defined val's type (not kind) while at it
            val valueType = symbol.tpe.typeSymbol // the type that this val instantiates.

            extractedModel.add(global)(valueType)
            //extractedModel.add(symbol.id, "is of type", valueType.id)

          /* 
           * Capture defs of methods.
           * Note this will also capture default constructors synthesized by the compiler
           * and synthetic accessor methods defined by the compiler for vals
           */ 
          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val symbol = tree.symbol

            extractedModel.add(global)(symbol)
            logParentIsOwner(symbol, parent = defParent.get)
            extractedModel.addIfUnique(defParent.get.id, "declares member", symbol.id)

            /*
            symbol.paramss.foreach { _.foreach { param =>
              extractedModel.add(global)(param)
              extractedModel.addIfUnique(symbol.id, "has parameter", param.id) 
              //println(s"parameter type is: ${param.tpe.typeSymbol} with ${param.tpe.typeArgs}")
            }}
            */
            
            /*
            symbol.typeParams.foreach { tparam =>
              extractedModel.add(global)(tparam)
              extractedModel.addIfUnique(symbol.id, "has type parameter", tparam.id)
              val TypeBounds(low, high) = tparam.tpe.bounds
              if (high.toString != "Nothing")
                extractedModel.add(global)(high.typeSymbol)
                println(s"has higher type bound ${high.typeSymbol.id}")
              if (low.toString != "Nothing")
                extractedModel.add(global)(low.typeSymbol)
                println(s"has lower type bound ${low.typeSymbol.id}")               
                //extractedModel.addIfUnique(tparam.id, "has lower type bound", low.toLongString)                
            }
            */
            
            val traverser = new ExtractionTraversal(Some(tree.symbol))
            traverser.traverse(rhs)

          /* Capture type definitions (classes, traits, objects) */
          case Template(parents, self, body) =>

            val typeSymbol = tree.tpe.typeSymbol

            extractedModel.add(global)(typeSymbol)
           
            val parentTypeSymbols = parents.map(parent => parent.tpe.typeSymbol).toSet
            parentTypeSymbols.foreach { s => extractedModel.add(global)(s) }


            if (defParent.isDefined) logParentIsOwner(typeSymbol, parent = defParent.get)
              
            parentTypeSymbols.foreach(s => {
              extractedModel.add(typeSymbol.id, "extends", s.id)
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

    performance.Counter.report
    
    extractedModel  
  }
}