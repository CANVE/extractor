package org.canve.githubCruncher
import scala.concurrent.{Future, Promise}
import java.util.{Timer => JavaTimer}
import java.util.TimerTask
import scala.util.Try

/*
 * A timer provided as an empty future that completes after a given time. 
 * The gory details: uses a java timer to complete a future returned by itself
 * code credit: http://stackoverflow.com/a/29691518/1509695 
 */
object Timer {
  def completeWhen(time: java.util.Date): Future[Unit] = {
    val promise = Promise[Unit]
    
    val javaTimer = new JavaTimer
    javaTimer.schedule(
      new TimerTask {
        override def run() = { promise.complete(Try(Unit)) }
      }, time)
    
    promise.future
  }
}