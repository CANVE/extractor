/*
 * Sbt plugin defining the `canve` custom sbt command, and managing
 * just-in-time injection of the canve compiler plugin.
 *
 * We do not use sbt's `addCompilerPlugin` api but rather inject the compiler plugin only when 
 * the `canve` command is run. (`addCompilerPlugin` will make sbt use the compiler plugin even 
 * during plain `sbt compile`, whereas the idea is to avoid such "pollution" and 
 * leave ordinary `sbt compile` untouched).
 *
 * TODO: refactor this long sequential code into a more reasonably modular form.
 */

package canve.sbt

import sbt.Keys._
import sbt._

// in case we want to add anything to the general cleanup task: http://www.scala-sbt.org/0.13.5/docs/Getting-Started/More-About-Settings.html#appending-with-dependencies-and

object Plugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  val compilerPluginOrg = "canve"
  val compilerPluginVersion = "0.0.1"
  val compilerPluginArtifact = "compiler-plugin"
  val compilerPluginNameProperty = "canve" // this is defined in the compiler plugin's code

  val sbtCommandName = "canve"

  val aggregateFilter: ScopeFilter.ScopeFilter = ScopeFilter( inAggregates(ThisProject), inConfigurations(Compile) ) // see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780

  /*
   * add the sbt `canve` command, and fetch the compiler dependency
   */
  override lazy val projectSettings = Seq(

    commands += Command.command(
      sbtCommandName,
      "Instruments all projects in the current build definition such that they run canve during compilation",
      "Instrument all projects in the current build definition such that they run canve during compilation")
      (inject().andThen(canve()))
  )

  /*
   *  
   */
  private def inject(): State => State = { state =>
    val extracted: Extracted = Project.extract(state)

    val enrichedLibDepSettings: Seq[Def.Setting[Seq[ModuleID]]] = extracted.structure.allProjectRefs map { projRef =>
      val projectName = projRef.project
      val projectScalaVersion = (scalaBinaryVersion in projRef)
      
      libraryDependencies in projRef += compilerPluginOrg % ("bla" + compilerPluginArtifact + "_" + projectScalaVersion.value) % compilerPluginVersion % "provided"
    }

    val newState = extracted.append(enrichedLibDepSettings, state)

    val updateAfterLibAppend = extracted.structure.allProjectRefs map { 
      projRef => println("running update: " + EvaluateTask(extracted.structure, update, newState, projRef)) }
      state
  }
  
  /*
   * Implementation of the `canve` command
   */
  private def canve(): State => State = { state =>

    org.canve.util.CanveDataIO.clearAll

    val extracted: Extracted = Project.extract(state)

    /*
     * Prepares settings that inject the compiler plugin through the dedicated scalac option
     * named -Xplugin, while taking care of additional scalac options required by it
     */
    val newSettings: Seq[Def.Setting[Task[Seq[String]]]] = extracted.structure.allProjectRefs map { projRef =>
      
      val projectName = projRef.project

      val projectScalaVersion = (scalaBinaryVersion in projRef)
      
      
      lazy val pluginScalacOptions: Def.Initialize[Task[Seq[String]]] = Def.task {

        libraryDependencies in projRef += compilerPluginOrg % (compilerPluginArtifact + "_" + projectScalaVersion.value) % compilerPluginVersion % "provided"
        
        // obtain the compiler plugin's file location to so it can be passed along to -Xplugin
        val providedDeps: Seq[File] = (update in projRef).value matching configurationFilter("provided")
        val pluginPath = providedDeps.find(_.getAbsolutePath.contains(compilerPluginArtifact))

        pluginPath match {

          case Some(pluginPath) =>

            println(s"picking up compiler plugin for project $projectName from path: " + pluginPath)
            pluginPath.getName.contains(s"${compilerPluginArtifact}_$projectScalaVersion") match {
              
              case false =>  
                println(s"Warn: skipping instrumentation for project $projectName (c.f. https://github.com/CANVE/extractor/issues/12)\n" +
                        s"pluginPath $pluginPath does not match target project's scala version: $projectScalaVersion")
                Seq() // avoid injecting the compiler plugin for the particular project at hand..
              
              case true =>
              
                val baseCompilerOptions =
                  Seq(
                    // hooks in the compiler plugin
                    Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),
                    // passes the name of the project being compiled, to the plugin
                    Some(s"-P:$compilerPluginNameProperty:projectName:$projectName")).flatten 
                
                val fullCompilerOptions =  
                  // enables obtaining accurate source ranges in the compiler plugin,
                  // will crash with some scala 2.10 projects which are using macros (c.f. https://github.com/scoverage/scalac-scoverage-plugin/blob/master/2.10.md)
                  baseCompilerOptions ++ Some(s"-Yrangepos")
                  
                projectScalaVersion.value match {
                  case "2.10" => baseCompilerOptions
                  case _      => fullCompilerOptions
                }
            }
            
          case None => throw new Exception(s"Fatal: canve sbt plugin failed injecting its compiler plugin: compiler plugin artifact not found among project's dependency resolved dependencies")
        }
      }
      scalacOptions in projRef ++= pluginScalacOptions.value
    }

    val appendedState = extracted.append(newSettings, state)

    /*
     * clean & compile all sbt projects
     */
    val successfulProjects = (for (projRef <- extracted.structure.allProjectRefs.toStream) yield {
      EvaluateTask(extracted.structure, clean, appendedState, projRef) match {
        case None =>
          throw new Exception("sbt plugin internal error - failed to evaluate the clean task")
        case _ =>
          EvaluateTask(extracted.structure, compile in Test, appendedState, projRef) match { // evaluating both src and test compilation
            case None =>
              throw new Exception("sbt plugin internal error - failed to evaluate the compile task")
            case Some((resultState, result)) => result.toEither match {
              case Left(incomplete: Incomplete) =>
                false
              case Right(analysis) =>
                true
            }
            case _ => throw new Exception("sbt plugin internal error - unexpected result from sbt api")
          }
      }
    }).takeWhile(_ == true).force

    /*
     * if compilation went all smooth for all sbt projects,proceed to normalize data across the projects
     */
    successfulProjects.length == extracted.structure.allProjectRefs.length match {
      case true =>
        println("normalizing data across subprojects...")
        val normalizedData = org.canve.compilerPlugin.normalization.CrossProjectNormalizer.apply
        println("canve task done")
        state
      case false =>
        println("canve task aborted as it could not successfully compile the project (or due to its own internal error)")
        state.fail
    }
  }

  println(s"[canve] sbt canve plugin loaded - enter `$sbtCommandName` in sbt, to run canve for your project")
}
