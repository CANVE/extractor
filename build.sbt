/*
 * Master build definition for CANVE data extraction.
 */

val integrationTest = taskKey[Unit]("Executes integration tests.")

lazy val root = (project in file("."))
  .aggregate(simpleGraph, compilerPluginUnitTestLib, canveCompilerPlugin, canveSbtPlugin, sbtPluginTestLib)
  .enablePlugins(CrossPerProjectPlugin) // makes sbt recursively respect cross compilation subproject versions, thus skipping compilation for versions that should not be compiled. (this is an sbt-doge global idiom).
  .settings(
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.4", "2.11.7"),
    publishArtifact := false, // no artifact to publish for the virtual root project
    integrationTest := (run in Compile in sbtPluginTestLib).toTask("").value
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
  .dependsOn(simpleGraph, compilerPluginUnitTestLib % "test")
  .settings(
    name := "compiler-plugin",
    organization := "canve",
    version := "0.0.1",
    isSnapshot := true, // to enable overwriting the existing artifact version
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.10.4", "2.11.7"),
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-library" % scalaVersion.value % "provided",
      "com.github.tototoshi" %% "scala-csv" % "1.2.2",
      //"com.github.tototoshi" %% "scala-csv" % "1.3.0-SNAPSHOT", // snapshot version might be causing sbt/ivy going crazy
      //"org.apache.tinkerpop" % "tinkergraph-gremlin" % "3.0.1-incubating",
      //"canve" %% "simple-graph" % "0.0.1",
      //"canve" %% "compiler-plugin-unit-test-lib" % "0.0.1" % "test",
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
  .settings(
    organization := "canve",
    name := "sbt-plugin",
    isSnapshot := true, // to enable overwriting the existing artifact version
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4"),
    sbtPlugin := true
    //resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    //libraryDependencies ++= Seq("com.github.tototoshi" %% "scala-csv" % "1.3.0-SNAPSHOT") 
  )

/*
 * Integration testing module, that runs our sbt module on select projects
 */
lazy val sbtPluginTestLib = (project in file("sbt-plugin-test-lib"))
  .dependsOn(canveCompilerPlugin) // TODO: This is currently just for a util object - we can do better.
  .enablePlugins(CrossPerProjectPlugin)
  .settings(
    name := "sbt-plugin-test-lib",
    organization := "canve",
    version := "0.0.1",

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
    
    publishArtifact := false,

    (run in Compile) <<= (run in Compile)
      .dependsOn(publishLocal in canveSbtPlugin)
      .dependsOn(publishLocal in canveCompilerPlugin)
  )

/*
 * And these depenency projects are developed as mostly generic libraries
 */
lazy val simpleGraph = (project in file("simple-graph"))
lazy val compilerPluginUnitTestLib = (project in file("compiler-plugin-unit-test-lib"))
