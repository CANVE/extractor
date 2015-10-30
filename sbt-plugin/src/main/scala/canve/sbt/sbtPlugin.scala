/*
 * Sbt plugin defining the `canve` custom sbt command, and managing
 * just-in-time injection of the canve compiler plugin.
 *
 * We do not use sbt's `addCompilerPlugin` api but rather inject
 * the compiler plugin only when the `canve` command is run.

 * (`addCompilerPlugin` will make sbt use the compiler plugin even during plain `sbt compile`,
 * whereas the idea is to avoid such "pollution" and leave ordinary `sbt compile` untouched).
 *
 * TODO: refactor this long sequential code into a more reasonably modular form during the next related session (separating into methods and objects...)
 */

package canve.sbt

import sbt.Keys._
import sbt._

import org.canve.compilerPlugin.Normalize

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
      (canve()),

    libraryDependencies += compilerPluginOrg % (compilerPluginArtifact + "_" + scalaBinaryVersion.value) % compilerPluginVersion % "provided"
  )

  /*
   * implementation of the `canve` command
   */
  private def canve(): State => State = { state =>

    org.canve.util.CanveDataIO.clearAll

    val extracted: Extracted = Project.extract(state)

    /*
     * prepare settings to inject the compiler plugin through the dedicated scalac option
     * named -Xplugin, while taking care of additional scalac options needed for it
     */
    val newSettings: Seq[Def.Setting[Task[Seq[String]]]] = extracted.structure.allProjectRefs map { projRef =>
      val projectName = projRef.project
      //println("canve instrumenting project " + projectName)

      lazy val pluginScalacOptions: Def.Initialize[Task[Seq[String]]] = Def.task {

        // extract the compiler plugin's path for -Xplugin
        val deps: Seq[File] = update.value matching configurationFilter("provided")
        deps.find(_.getAbsolutePath.contains(compilerPluginArtifact)) match {

          case Some(pluginPath) =>
            Seq(

              // enable obtaining accurate source ranges in the compiler plugin,
              // will crash with some scala 2.10 projects using macros (c.f. https://github.com/scoverage/scalac-scoverage-plugin/blob/master/2.10.md)
              Some(s"-Yrangepos"),

              // hook in the compiler plugin
              Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),

              // pass the name of the project being compiled, to the plugin
              Some(s"-P:$compilerPluginNameProperty:projectName:$projectName")

            ).flatten

          case None => throw new Exception(s"Fatal: compilerPluginArtifact not in libraryDependencies")

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
          EvaluateTask(extracted.structure, compile in Test, appendedState, projRef) match {
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
        new Normalize
        println("canve task done")
        state
      case false =>
        println("canve task aborted as it could not successfully compile the project (or due to its own internal error)")
        state.fail
    }
  }

  println(s"[canve] sbt canve plugin loaded - enter `$sbtCommandName` in sbt, to run canve for your project")
}