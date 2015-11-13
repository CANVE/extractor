lazy val project: Project = Project("scalaPlus", file(".")).settings(
  name := "scalaPlus",
  organization := "canve",
  version := "0.0.1",
  isSnapshot := true, // to enable overwriting the existing artifact version
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-library" % scalaVersion.value % "provided"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework")
)