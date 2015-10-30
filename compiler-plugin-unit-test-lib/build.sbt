lazy val compilerPluginUnitTestLib = (project in file(".")).settings(
  organization := "canve",
  name := "compiler-plugin-unit-test-lib",
  isSnapshot := true, // to enable overwriting the existing artifact version
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.4", "2.11.7"),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
))
