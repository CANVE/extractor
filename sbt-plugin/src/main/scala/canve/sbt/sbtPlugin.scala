/*
 * Sbt plugin defining the `canve` custom sbt command, and managing
 * just-in-time injection of the canve compiler plugin.
 *
 * We do not use sbt's `addCompilerPlugin` api but rather inject the compiler plugin only when 
 * the `canve` command is run. (`addCompilerPlugin` will make sbt use the compiler plugin even 
 * during plain `sbt compile`, whereas the idea is to avoid such "pollution" and 
 * leave ordinary `sbt compile` untouched).
 *
 * TODO: refactor this long sequential code into a reasonably more modular form.
 * TODO: consider simplifying injection per https://github.com/scala/scala-dist-smoketest/commit/b4a342a26883d7e10cc7a28918d30b664e23f466
 * TODO: in case we want to add anything to the general cleanup task: http://www.scala-sbt.org/0.13.5/docs/Getting-Started/More-About-Settings.html#appending-with-dependencies-and
 */

package canve.sbt

import sbt.Keys._
import sbt._
import org.canve.shared.DataWithLog
import org.canve.logging.loggers._
import java.io.File
import play.api.libs.json._
//import com.github.nscala_time.time.Imports._ crashes at runtime over java.lang.NoClassDefFoundError: com/github/nscala_time/time/Imports
import org.canve.compilerPlugin.normalization.CrossProjectNormalizer.normalize
import scala.util.{Try, Success, Failure}

object Plugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  val compilerPluginOrg = buildInfo.BuildInfo.organization
  val compilerPluginVersion = "0.0.1"
  val compilerPluginArtifact = "compiler-plugin"
  val compilerPluginNameProperty = "canve" // this is defined in the compiler plugin's code

  val sbtCommandName = "canve"

  val aggregateFilter: ScopeFilter.ScopeFilter = ScopeFilter( inAggregates(ThisProject), inConfigurations(Compile) ) // see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780

  /*
   * add the sbt `canve` command, and fetch the compiler dependency
   */
  override lazy val projectSettings = Seq(

    libraryDependencies += compilerPluginOrg % (compilerPluginArtifact + "_" + scalaBinaryVersion.value) % compilerPluginVersion % "provided",  

    commands += Command.args(
      sbtCommandName,
      ("Instrument all projects in the current build definition such that they run canve during compilation",
      "Instrument all projects in the current build definition such that they run canve during compilation"),
      "Instrument all projects in the current build definition such that they run canve during compilation",
      "Instrument all projects in the current build definition such that they run canve during compilation")
      (canve())
      // if only not https://github.com/CANVE/extractor/issues/12: (perProjectInject().andThen(canve()))
  )

  /*
   * This does not work (http://stackoverflow.com/q/34253338/1509695), so not used for now,
   * and we silently skip sub-projects that are not same scala version as the overall build definition (c.f. https://github.com/CANVE/extractor/issues/12)
   */
  private def perProjectInject(): State => State = { state =>
    val extracted: Extracted = Project.extract(state)

    val enrichedLibDepSettings = extracted.structure.allProjectRefs map { projRef =>
    
      val projectScalaVersion = (scalaBinaryVersion in projRef)
      
      libraryDependencies in projRef += 
        compilerPluginOrg % (compilerPluginArtifact + "_" + projectScalaVersion.value) % compilerPluginVersion
    }

    val appendedState = extracted.append(enrichedLibDepSettings, state)

    val pluginFetching = (for (projRef <- extracted.structure.allProjectRefs.toStream) yield {
      EvaluateTask(extracted.structure, update, appendedState, projRef) match {
        case None =>
          throw new Exception("sbt plugin internal error - failed to evaluate the update task")

        case Some((resultState, result)) => result.toEither match {
          case Left(incomplete: Incomplete) =>
            false
          case Right(analysis) =>
            true
        }
      }
    }).takeWhile(_ == true).force     

    state
  }

  /*
   * Implementation of the `canve` command
   */
  private def canve(): (State, Seq[String]) => State = { (state, args) =>
      
    if (args.length > 1) throw new Exception("too many arguments supplied to the sbt canve command ― zero or one arguments expected") 
    
    // val startTime: DateTime = DateTime.now 
    
    val outputPath = args.isEmpty match {
      case true  => new DataWithLog("canve-data") // default value
      case false => new DataWithLog(args.head) 
    }
    
    object Log extends StringLogger(outputPath.logDir + File.separator + "sbtPlugin.log")
    object DataLog extends JsonLogger(outputPath.dataDir.toString)  

    def dataLogResult(success: Boolean, reason: String = "") {
      //val elapsed: Duration = (startTime to DateTime.now).toDuration
      DataLog(
        "sbtPlugin", 
        success match {
          case true  => Json.parse(s""" { "completion" : "normal" } """)
          case false => Json.parse(s""" { "completion" : "abnormal", "reason" : "$reason" } """)
        }
      )
    }
    
    new org.canve.shared.DataIO(outputPath.dataDir.toString).clearAll

    val extracted: Extracted = Project.extract(state)

    /*
     * Prepares settings that inject the compiler plugin through the dedicated scalac option
     * named -Xplugin, while taking care of additional scalac options required by it
     */
    val newSettings: Seq[Def.Setting[Task[Seq[String]]]] = extracted.structure.allProjectRefs map { projRef =>
      
      val projectName = projRef.project

      val projectScalaBinaryVersion = (scalaBinaryVersion in projRef)
      
      lazy val pluginScalacOptions: Def.Initialize[Task[Seq[String]]] = Def.task {
        
        scala.tools.nsc.io.File(outputPath.dataDir + File.separator + "handling").createDirectory()        
        def dataLogSubProjectHandling(handling: String) {
          DataLog(
            s"handling/$projectName", 
            Json.parse(s"""
              {
                "handling" : "$handling",
                "projectScalaBinaryVersion" : "${projectScalaBinaryVersion.value}",
                "overallScalaBinaryVersion" : "${scalaBinaryVersion.value}"
              }
            """))
        }

        // obtain the compiler plugin's file location to so it can be passed along to -Xplugin
        val providedDeps: Seq[File] = (update in projRef).value  matching configurationFilter("provided")
        //Log(providedDeps.toString)
        //Log(projectScalaBinaryVersion.value)
        //Log(scalaBinaryVersion.value)
        val pluginPath = providedDeps.find(_.getAbsolutePath.contains(compilerPluginArtifact))
       
        pluginPath match {

          case Some(pluginPath) =>
            
            (projectScalaBinaryVersion.value) == scalaBinaryVersion.value match {
              
              case false =>  
                // avoid injecting the compiler plugin for the particular project at hand..
                // perhaps the user can still manually add the necessary compiler plugin for those skipped projects, 
                // through http://www.scala-sbt.org/0.13/docs/Compiler-Plugins.html or otherwise.
                dataLogSubProjectHandling("skip (compiler plugin version inconsistency)")
                Log(
                  s"Warn: skipping instrumentation for sub-project $projectName (c.f. https://github.com/CANVE/extractor/issues/12)\n" + 
                  s"sub-project scala version is ${projectScalaBinaryVersion.value} while overall project version is ${scalaBinaryVersion.value}")
                Seq() 
              
              case true =>
                dataLogSubProjectHandling("attempt")
                val baseCompilerOptions =
                  Seq(
                    // hooks in the compiler plugin
                    Some(s"-Xplugin:${pluginPath.getAbsolutePath}"),
                    // passes the name of the project being compiled, to the plugin
                    Some(s"-P:$compilerPluginNameProperty:projectName:$projectName"),
                    // passes the data output path argument
                    Some(s"-P:$compilerPluginNameProperty:outputDataPath:${outputPath.base}")
                  ).flatten 
                
                val fullCompilerOptions =  
                  // enables obtaining accurate source ranges in the compiler plugin,
                  // will crash with some scala 2.10 projects which are using macros (c.f. https://github.com/scoverage/scalac-scoverage-plugin/blob/master/2.10.md)
                  baseCompilerOptions ++ Some(s"-Yrangepos")
                  
                projectScalaBinaryVersion.value match {
                  case "2.10" => baseCompilerOptions
                  case _      => fullCompilerOptions
                }
            }
            
          case None =>
            dataLogSubProjectHandling("skip (compiler plugin unavailable)")
            // Observed for akka-2.4.1. Assuming (but not confirmed) it might happen if sub-project overrides (and therefore lacks) root project's librarySettings
            Log(s"Warn: skipping instrumentation for sub-project $projectName: compiler plugin artifact not available among project's resolved dependencies")
            Seq()
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
        Log("normalizing data across subprojects...")
      
        Try(normalize(outputPath.dataDir.toString)) match {
          case Failure(e) => 
            Log(s"canve task aborted during subprojects normalization: $e") 
            dataLogResult(false, "failed at subprojects normalization")
            state.fail
          case Success(_) => 
            Log("canve task done")
            dataLogResult(true)
            state
        }
        
      case false =>
        Log("canve task aborted as it could not successfully compile the project, or due to its own internal error")
        dataLogResult(false, "failed during compilation of the project")
        state.fail
    }
  }

  println(s"[canve] sbt canve plugin loaded - enter `$sbtCommandName` in sbt, to run canve for your project")
}
