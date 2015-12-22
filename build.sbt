/*
 * Master build definition for CANVE data extraction.
 *
 * Note:
 *
 *   The default is not to build for 2.12, as some dependencies
 *   are not yet published for 2.12 milestone releases. To cross build
 *   also for 2.12, run sbt like so: sbt -Dscala12=true.
 *
 *   Ask us for a hacked 2.12 compilation of the external dependencies if
 *   you'd like to use this option.
 *
 */

/*
 * Import for https://github.com/agemooij/sbt-prompt, providing an informative multi-project sbt prompt.
 * You will need to follow the instructions there for setting up the fancy unicode symbols which it
 * uses for display. In particular you'll also need to configure your terminal preferences to use
 * one of the fonts from https://github.com/powerline/fonts.
 */
import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._

/*
 * allow overriding the org name
 */
lazy val org = sys.props.getOrElse("org", "canve")

/*
 * Optional 2.12 milestone building
 */
val scala12release = "2.12.0-M2" // pre-release scala 2.12 version to use

lazy val scala12 = sys.props.getOrElse("scala12", "false")

lazy val commonCrossScalaVersions = scala12 match {
  case "false" => Seq("2.10.4", "2.11.7")
  case "true"  => Seq("2.10.4", "2.11.7", scala12release)
  case _ => throw new Exception("invalid argument value supplied for scala12 (can only be \"true\", or unspecified altogether)")
}

/*
 * the integration test custom task
 */
val integrationTest = taskKey[Unit]("Executes integration tests.")

/*
 * the root project definition
 */
lazy val root = (project in file("."))
  .aggregate(simpleLogging, simpleGraph, compilerPluginUnitTestLib, canveCompilerPlugin, canveSbtPlugin, integrationTestProject)
  .enablePlugins(CrossPerProjectPlugin) // makes sbt recursively respect cross compilation subproject versions, thus skipping compilation for versions that should not be compiled. (this is an sbt-doge global idiom).
  .settings(
    promptTheme := Scalapenos,
    scalaVersion := "2.11.7",
    crossScalaVersions := commonCrossScalaVersions,
    publishArtifact := false, // no artifact to publish for the virtual root project
    integrationTest := (run in Compile in integrationTestProject).toTask("").value
)

/*
 * The compiler plugin module. Note we cannot call it simply
 * `CompilerPlugin` as that name is already taken by sbt itself
 *
 * It uses the assembly plugin to stuff all its dependencies into its artifact,
 * otherwise the way it gets injected into user builds, their classes will be
 * missing at compile time.
 */
lazy val canveCompilerPlugin = (project in file("compiler-plugin"))
  .dependsOn(simpleLogging, simpleGraph, compilerPluginUnitTestLib % "test")
  .settings(
    name := "compiler-plugin",
    organization := org,
    version := "0.0.1",
    promptTheme := Scalapenos,
    isSnapshot := true, // to enable overwriting the existing artifact version
    scalaVersion := "2.11.7",
    //scalacOptions ++= Seq("-Ymacro-debug-lite"),
    crossScalaVersions := commonCrossScalaVersions,
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-library" % scalaVersion.value % "provided",
      //"com.github.tototoshi" %% "scala-csv" % "1.2.2",
      "com.github.tototoshi" %% "scala-csv" % "1.3.0-SNAPSHOT", // snapshot version might be causing sbt/ivy going crazy
      //"org.apache.tinkerpop" % "tinkergraph-gremlin" % "3.0.1-incubating",
      //"canve" %% "simple-graph" % "0.0.1",
      //"canve" %% "compiler-plugin-unit-test-lib" % "0.0.1" % "test",
      //"com.lihaoyi" %% "pprint" % "0.3.6",
      "com.lihaoyi" %% "utest" % "0.3.1" % "test"
    ),

    testFrameworks += new TestFramework("utest.runner.Framework"),

    /*
     * take care of including all non scala core library dependencies in the build artifact
     */

    test in assembly := {},

    jarName in assembly := name.value + "_" + scalaVersion.value + "-" + version.value + "-assembly.jar",
    assemblyOption in assembly ~= { _.copy(includeScala = false) },
    packagedArtifact in Compile in packageBin := {
      val temp = (packagedArtifact in Compile in packageBin).value
      //println(temp)
      val (art, slimJar) = temp
      val fatJar = new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      println("Using sbt-assembly to package library dependencies into a fat jar for publication")
      (art, slimJar)
    }
  )

/*
 * The sbt plugin module. It adds the compiler plugin module to user project compilations
 */
lazy val canveSbtPlugin = (project in file("sbt-plugin"))
  .dependsOn(canveCompilerPlugin)
  .enablePlugins(CrossPerProjectPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    organization := org,
    name := "sbt-plugin",
    isSnapshot := true, // to enable overwriting the existing artifact version
    promptTheme := Scalapenos,
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4"),
    sbtPlugin := true,

    /* make the organization name available inside a dedicated object during the build */
    buildInfoKeys := Seq[BuildInfoKey](organization),
    buildInfoObject := "BuildInfo",
    buildInfoPackage := "buildInfo"

    //resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    //libraryDependencies ++= Seq("com.github.tototoshi" %% "scala-csv" % "1.3.0-SNAPSHOT")
  )

/*
 * Integration testing module, that runs our sbt module on select projects
 */
lazy val integrationTestProject = (project in file("sbt-plugin-integration-test"))
  .dependsOn(canveCompilerPlugin) // TODO: This is currently just for a util object - we can do better.
  .enablePlugins(CrossPerProjectPlugin)
  .settings(
    name := "sbt-plugin-test-lib",
    organization := org,
    version := "0.0.1",
    promptTheme := Scalapenos,

    /*
     * this project is purely running sbt as an OS process, so it can use latest scala version not sbt's scala version,
     * and there is no need whatsoever to provided a cross-compilation of it for older scala.
     */
    scalaVersion := "2.11.7",

    /*
     * The following resolver is added as a workaround: the `update task` of this subproject,
     * may oddly enough try to resolve scala-csv, which in turn may fail if the resolver for it is not in scope here.
     */
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided", // otherwise cannot use scala.tools.nsc.io.File
      "org.fusesource.jansi" % "jansi" % "1.4"
    ),

    publishArtifact := false

    //(run in Compile) <<= (run in Compile).dependsOn(publishLocal in canveSbtPlugin).dependsOn(publishLocal in canveCompilerPlugin) // https://github.com/CANVE/extractor/issues/2
  )

/*
 * And these depenency projects are developed (generally speaking) as generic libraries
 */
lazy val simpleGraph: Project = (project in file("simple-graph"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "simple-graph",
    organization := org,
    version := "0.0.1",
    promptTheme := Scalapenos,
    isSnapshot := true, // to enable overwriting the existing artifact version
    scalaVersion := "2.11.7",
    crossScalaVersions := commonCrossScalaVersions,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= Seq("com.lihaoyi" %% "utest" % "0.3.1" % "test"),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val compilerPluginUnitTestLib = (project in file("compiler-plugin-unit-test-lib")).settings(
  organization := org,
  name := "compiler-plugin-unit-test-lib",
  promptTheme := Scalapenos,
  isSnapshot := true, // to enable overwriting the existing artifact version
  scalaVersion := "2.11.7",
  crossScalaVersions := commonCrossScalaVersions,
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
))

/*
 * Not really a dependency, for now
 */
lazy val scalaPlus = (project in file("scala-plus")).settings(
  promptTheme := Scalapenos,
  scalaVersion := "2.11.7",
  publishArtifact := false
)

lazy val simpleLogging = (project in file("simple-logging")).settings(
  name := "simple-logging",
  organization := org,
  version := "0.0.1",
  promptTheme := Scalapenos,
  isSnapshot := true, // to enable overwriting the existing artifact version
  scalaVersion := "2.11.7",
  crossScalaVersions := commonCrossScalaVersions,
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    //"org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
    //"com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.0-1",
    "io.circe" %% "circe-core" % "0.2.1",
    "io.circe" %% "circe-generic" % "0.2.1",
    "io.circe" %% "circe-parse" % "0.2.1",
    "com.lihaoyi" %% "pprint" % "0.3.6",
    "com.lihaoyi" %% "utest" % "0.3.1" % "test"),
  testFrameworks += new TestFramework("utest.runner.Framework")
)

/*
 * Sound notifications for the patient - not yet working

 sound.play(compile in Compile, Sounds.Blow, Sounds.Tink)
 sound.play(test in Test, Sounds.Blow, Sounds.Ping)
 sound.play(publishLocal, Sounds.Blow, Sounds.Ping)
 */
