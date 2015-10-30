lazy val project: Project = Project("simpleGraph", file(".")).settings(
  name := "simple-graph",
  organization := "canve",
  version := "0.0.1",
  isSnapshot := true, // to enable overwriting the existing artifact version
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.4", "2.11.7"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  libraryDependencies ++= Seq("com.lihaoyi" %% "utest" % "0.3.1" % "test"),
  testFrameworks += new TestFramework("utest.runner.Framework")
)