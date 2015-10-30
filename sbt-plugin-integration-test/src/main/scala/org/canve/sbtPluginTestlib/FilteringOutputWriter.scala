package org.canve.sbtPluginTestLib

import java.io.{PrintWriter, BufferedWriter, OutputStreamWriter, FileOutputStream, Closeable, Flushable}
import org.fusesource.jansi.AnsiOutputStream
import scala.sys.process._
import java.io.File
import util.PrintUtil._

/*
 * Takes care of routing a process's stdout and stderr to a file, being a proper 
 * ProcessorLogger callback object for Scala's ProcessBuilder methods. Inspired by 
 * the original FileProcessorLogger in scala.sys.process.
 */

class FilteringOutputWriter(outFile: File, timeString: String) 
  extends ProcessLogger with Closeable with Flushable {
  
  val fileOutputStream = new FileOutputStream(outFile, true)
  
  private val writer = (
    new PrintWriter(
      new BufferedWriter(
        new OutputStreamWriter(
          new AnsiOutputStream(fileOutputStream)
        )
      )
    )
  )  
  
  writer.println(wrap("Following is the stdout and stderr output of the sbt process started on " + timeString))
  
  def out(s: ⇒ String): Unit = {
    writer.println(s)
    print(".")  
  }
  
  def err(s: ⇒ String): Unit = {
    writer.println("<error> " + s)
    print("..error..")  
  }
  
  def buffer[T](f: => T): T = f
  
  def close(): Unit = writer.close()
  def flush(): Unit = writer.flush()
}
