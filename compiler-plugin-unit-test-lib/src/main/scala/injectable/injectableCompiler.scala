/** @author of original code - Stephen Samuel */
package compilerPluginUnitTest

import java.io.{File, FileNotFoundException}
import java.net.URL
import scala.collection.mutable.ListBuffer
import scala.tools.nsc.{Settings, Global, Phase}
import scala.tools.nsc.plugins.PluginComponent

/*
 * a component to run over a compilation AST,
 * as the payload of compiler plugin which
 * will call it
 */
trait Injectable {
  def apply(global: Global)(body: global.Tree)
}

/*
 * InjectingCompiler factory object, 
 * taking care of compiler settings
 * before instantiating
 * 
 * justification for needing this factory object: 
 * http://stackoverflow.com/questions/32885498/can-scala-accept-any-derivation-of-a-tuple-as-an-argument-list
 */
object InjectingCompilerFactory {

  private lazy val runTimeScalaVersion: Option[String] = {
    val matcher = """version (\d+\.\d+\.\d+).*""".r
    scala.util.Properties.versionString match {
      case matcher(vsn) => Some(vsn)
      case _ => None
    }
  }
    
  if (runTimeScalaVersion.isEmpty) throw new Exception("Could not obtain scala runtime version. Are you running scala 2.9 or older?")
  
  private val ShortScalaVersion = runTimeScalaVersion.get.dropRight(2)

  private def classPath = getScalaJars.map(_.getAbsolutePath) // :+ sbtCompileDir.getAbsolutePath // :+ runtimeClasses.getAbsolutePath

  private def getScalaJars: List[File] = {
    val scalaJars = List("scala-compiler", "scala-library", "scala-reflect")
    scalaJars.map(findScalaJar)
  }

  private def sbtCompileDir: File = {
    val dir = new File("./target/scala-" + ShortScalaVersion + "/classes")
    if (!dir.exists)
      throw new FileNotFoundException(s"Could not locate SBT compile directory for plugin files [$dir]")
    dir
  }

  // private def runtimeClasses: File = new File("./scalac-scoverage-runtime/target/scala-2.11/classes")

  private def findScalaJar(artifactId: String): File = findIvyJar("org.scala-lang", artifactId, runTimeScalaVersion.get)

  private def findIvyJar(groupId: String, artifactId: String, version: String): File = {
    val userHome = System.getProperty("user.home")
    val sbtHome = userHome + "/.ivy2"
    val jarPath = sbtHome + "/cache/" + groupId + "/" + artifactId + "/jars/" + artifactId + "-" + version + ".jar"
    val file = new File(jarPath)
    if (!file.exists)
      throw new FileNotFoundException(s"Could not locate [$jarPath].")
    file
  }
  
  def apply(unitTestObj: Injectable): InjectingCompiler = {
    
    def settings: Settings = {
      val s = new scala.tools.nsc.Settings
      s.classpath.value = classPath.mkString(File.pathSeparator)
  
      val path = s"./target/scala-$ShortScalaVersion/test-generated-classes"
      new File(path).mkdirs()
      s.d.value = path
      s
    }
    
    val reporter = new scala.tools.nsc.reporters.ConsoleReporter(settings)
    
    new InjectingCompiler(settings, reporter, unitTestObj)
  }
}

/*
 * class that manages compilation of code provided to it,
 * invoking an @injectable after the compilation.
 */
class InjectingCompiler(settings: scala.tools.nsc.Settings, 
                       reporter: scala.tools.nsc.reporters.Reporter,
                       injectable: Injectable) extends scala.tools.nsc.Global(settings, reporter) {

  def compileCodeSnippet(code: String): InjectingCompiler = compileSourceFiles(writeCodeSnippetToTempFile(code))

  private def compileSourceFiles(files: File*): InjectingCompiler = {
    val command = new scala.tools.nsc.CompilerCommand(files.map(_.getAbsolutePath).toList, settings)
    new Run().compile(command.files)
    this
  }

  private def writeCodeSnippetToTempFile(code: String): File = {
    val file = File.createTempFile("canve-compiler-plugin-unit-test-tmp", ".scala")
    IOUtils.writeToFile(file, code)
    file.deleteOnExit()
    file
  }
  
  private def compileSourceResources(urls: URL*): InjectingCompiler = {
    compileSourceFiles(urls.map(_.getFile).map(new File(_)): _*)
  }

  /*
   * a standard PluginComponent, which executes the component supplied
   * to it, after the typical compiler plugin ceremony
   */
  private class Plugin(val global: Global) extends PluginComponent {

    val runsAfter = List("typer")

    override val runsRightAfter = Some("typer")
  
    val phaseName = "canve-unit-tester"
    
    
    override def newPhase(prev: Phase): Phase = new Phase(prev) {
      def name : String = phaseName 
      override def run() {
        
        println(Console.BLUE + Console.BOLD + "\ncanve unit test running" + Console.RESET)
        
        def units = global.currentRun
                    .units
                    .toSeq
                    .sortBy(_.source.content.mkString.hashCode())
        
        units.foreach { unit =>
          injectable.apply(global)(unit.body)
          println(Console.BLUE + "canve unit testing plugin examining source file" + unit.source.path + "..." + Console.RESET)
        }
      }
    }
  }

  /*
   * an override to the canonical compilation sequence:
   * it invokes all standard compiler phases up to the typer phase,
   * after which point the supplied compiler plugin is invoked, and no more.  
   */
  override def computeInternalPhases() {
    val phs = List(
      syntaxAnalyzer -> "parse source into ASTs, perform simple desugaring",
      analyzer.namerFactory -> "resolve names, attach symbols to named trees",
      analyzer.packageObjects -> "load package objects",
      analyzer.typerFactory -> "the meat and potatoes: type the trees",
      new Plugin(this) -> "the plugin injector"
    )
    phs foreach (addToPhasesSet _).tupled
  }
}


