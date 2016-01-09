package org.canve.sbtPluginTest
import org.canve.shared.Execution.TaskResultType

case class Project(dirObj: java.io.File, name: String)

case class Result(project: Project, result: TaskResultType, elapsed: Long)
