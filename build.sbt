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

lazy val commonSettings = Seq(
  promptTheme := Scalapenos,
  organization := org,
  publishArtifact in (Compile, packageDoc) := false,
  // disable publishing the main sources jar
  publishArtifact in (Compile, packageSrc) := false,

  /* workaround for failed snapshot resolution https://github.com/sbt/sbt/issues/1780 */
  resolvers += Resolver.sonatypeRepo("snapshots"),

  cancelable in Global := true // makes ctrl+c stop the current task rather than quit sbt
)

/*
 * the integration test custom task
 */
val integrationTest = taskKey[Unit]("Executes integration tests.")

/*
 * the root project definition
 */
lazy val root = (project in file("."))
  .aggregate(
    simpleLogging,
    canveShared,
    simpleGraph,
    compilerPluginUnitTestLib,
    canveCompilerPlugin,
    dataNormalizer,
    canveSbtPlugin,
    integrationTestProject,
    githubCruncher)
  .enablePlugins(CrossPerProjectPlugin) // makes sbt recursively respect cross compilation subproject versions, thus skipping compilation for versions that should not be compiled. (this is an sbt-doge global idiom).
  .settings(commonSettings).settings(
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
  .dependsOn(canveShared, simpleLogging, simpleGraph, compilerPluginUnitTestLib % "test")
  .settings(commonSettings).settings(
    name := "compiler-plugin",
    version := "0.0.1",
    isSnapshot := true, // to enable overwriting the existing artifact version during dev
    scalaVersion := "2.11.7",
    //scalacOptions ++= Seq("-Ymacro-debug-lite"),
    crossScalaVersions := commonCrossScalaVersions,
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.sonatypeRepo("snapshots"),

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-library" % scalaVersion.value % "provided",
      //"com.github.tototoshi" %% "scala-csv" % "1.2.2",
      "com.github.tototoshi" %% "scala-csv" % "1.3.0-SNAPSHOT", // snapshot version might summon some sbt bugs
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
  .settings(commonSettings).settings(
    name := "sbt-plugin",
    isSnapshot := true, // to enable overwriting the existing artifact version during dev
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4"),
    sbtPlugin := true,

    /* Generate source code that includes the organization name, to be included in compilation */
    buildInfoKeys := Seq[BuildInfoKey](organization),
    buildInfoPackage := "buildInfo",
    buildInfoObject := "BuildInfo",
    EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Managed
  )

/*
 * Data normalization module
 */
lazy val dataNormalizer = (project in file("data-normalizer"))
  .dependsOn(canveCompilerPlugin)
  .enablePlugins(CrossPerProjectPlugin)
  .settings(commonSettings).settings(
    name := "data-normalizer",
    isSnapshot := true, // to enable overwriting the existing artifact version during dev
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.11.7"),
    publishArtifact := false
  )

/*
 * Library for shared low-level components and utility functions
 */
lazy val canveShared = (project in file("canve-shared"))
  .settings(commonSettings).settings(
    name := "canve-shared",
    version := "0.0.1",
    isSnapshot := true, // to enable overwriting the existing artifact version during dev
    publishArtifact := false,
    scalaVersion := "2.11.7",
    crossScalaVersions := commonCrossScalaVersions,

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided", // otherwise cannot use scala.tools.nsc.io.File
      "org.fusesource.jansi" % "jansi" % "1.4"
    )
  )

/*
 * Integration testing module, that runs our sbt module on select projects
 */
lazy val integrationTestProject = (project in file("sbt-plugin-integration-test"))
  .dependsOn(canveShared)
  .enablePlugins(CrossPerProjectPlugin)
  .settings(commonSettings).settings(
    name := "sbt-plugin-integration-test",
    version := "0.0.1",

    /*
     * this project is purely running sbt as an OS process, so it can use latest scala version not sbt's scala version,
     * and there is no need whatsoever to provided a cross-compilation of it for older scala.
     */
    scalaVersion := "2.11.7",

    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided", // otherwise cannot use scala.tools.nsc.io.File
      "org.fusesource.jansi" % "jansi" % "1.4"
    ),

    publishArtifact := false

    //(run in Compile) <<= (run in Compile).dependsOn(publishLocal in canveSbtPlugin).dependsOn(publishLocal in canveCompilerPlugin) // https://github.com/CANVE/extractor/issues/2
  )

/*
 * Github crunching pipeline
 */
lazy val githubCruncher = (project in file("github-cruncher"))
 .dependsOn(canveShared)
 .enablePlugins(CrossPerProjectPlugin)
 .enablePlugins(BuildInfoPlugin).settings(
   /* Generate source code that includes the organization name, to be included in compilation */
   buildInfoKeys := Seq[BuildInfoKey](baseDirectory),
   buildInfoPackage := "SbtOwnBuildInfo",
   buildInfoObject  := "info",
   EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Managed
 )
 .settings(commonSettings)
 .settings(
   scalaVersion := "2.11.7",
   crossScalaVersions := Seq("2.11.7"),
   publishArtifact := false,

   /* allenai pipeline */
   resolvers += Resolver.bintrayRepo("allenai", "maven"),
   libraryDependencies += "org.allenai" %% "pipeline" % "1.4.24",

   libraryDependencies ++= Seq(

     "com.github.nscala-time" %% "nscala-time" % "2.6.0",

     /* slick */
     "com.typesafe.slick" %% "slick" % "3.1.1",
     "org.slf4j" % "slf4j-nop" % "1.6.4",
     "com.typesafe.slick" %% "slick-codegen" % "3.1.1",
     "mysql" % "mysql-connector-java" % "5.1.38",
     "com.zaxxer" % "HikariCP-java6" % "2.3.9",

     /* json */
     "com.typesafe.play" %% "play-json" % "2.4.6",

     /* http client */
     "org.scalaj" %% "scalaj-http" % "2.2.0"
  ),

  /* storm */
  resolvers ++= Seq("clojars" at "http://clojars.org/repo/",
                    "clojure-releases" at "http://build.clojure.org/releases"),
  libraryDependencies += "org.apache.storm" % "storm-core" % "0.10.0" % "provided",

  /* register slick sbt command */
  slickAutoGenerate <<= slickCodeGenTask
  // sourceGenerators in Compile <+= slickCodeGenTask // register automatic code generation on every compile
)

 // slick code generation task - from https://github.com/slick/slick-codegen-example/blob/master/project/Build.scala
 lazy val slickAutoGenerate = TaskKey[Seq[File]]("slick-gen")

 lazy val slickCodeGenTask = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>

   val dbName = "github_crawler"
   val (user, password) = ("canve", "") // no password for this user

   val jdbcDriver = "com.mysql.jdbc.Driver"
   val slickDriver = "slick.driver.MySQLDriver"
   val url = s"jdbc:mysql://localhost:3306/$dbName?user=$user"

   val targetDir = "src/main/scala"
   val pkg = "org.canve.githubCruncher.mysql"

   toError(r.run("slick.codegen.SourceCodeGenerator", cp.files, Array(slickDriver, jdbcDriver, url, targetDir, pkg), s.log))

   val outputSourceFile = s"$targetDir/org/canve/githubCruncher/mysql/Tables.scala"
   println(scala.Console.GREEN + s"[info] slick code now auto-generated at $outputSourceFile" + scala.Console.RESET)
   Seq(file(outputSourceFile))
 }

/*
 * And these depenency projects are developed (generally speaking) as generic libraries
 */
lazy val simpleGraph: Project = (project in file("simple-graph"))
  .settings(commonSettings).settings(
    name := "simple-graph",
    version := "0.0.1",
    isSnapshot := true, // to enable overwriting the existing artifact version during dev
    scalaVersion := "2.11.7",
    crossScalaVersions := commonCrossScalaVersions,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= Seq("com.lihaoyi" %% "utest" % "0.3.1" % "test"),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val compilerPluginUnitTestLib = (project in file("compiler-plugin-unit-test-lib")).settings(commonSettings).settings(

  name := "compiler-plugin-unit-test-lib",
  isSnapshot := true, // to enable overwriting the existing artifact version during dev
  scalaVersion := "2.11.7",
  crossScalaVersions := commonCrossScalaVersions,
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
))

lazy val simpleLogging = (project in file("simple-logging")).settings(commonSettings).settings(
  name := "simple-logging",
  version := "0.0.1",
  isSnapshot := true, // to enable overwriting the existing artifact version during dev
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
 * Not really a dependency, for now
 */
lazy val scalaPlus = (project in file("scala-plus")).settings(commonSettings).settings(
  scalaVersion := "2.11.7",
  publishArtifact := false
)
