package org.canve.sbtPluginTestLib

case class Project(dirObj: java.io.File, name: String)

abstract class ResultType
object Okay    extends ResultType
object Failure extends ResultType
object Skipped extends ResultType

case class Result(project: Project, result: ResultType, elapsed: Long)
