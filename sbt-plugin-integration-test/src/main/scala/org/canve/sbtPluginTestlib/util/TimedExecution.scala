package org.canve.sbtPluginTestLib.util

case class TimedExecutionResult[T](result: T, elapsed: Long)

/*
 * Runs an input function and times its execution
 */
object TimedExecution {
  def apply[T](func: => T) = {
    val start = System.nanoTime()
    val result = func // invoke the argument function
    val elapsed = (System.nanoTime() - start) / 1000 / 1000 // elapsed time in milliseconds
    TimedExecutionResult(result, elapsed)
  }
}
