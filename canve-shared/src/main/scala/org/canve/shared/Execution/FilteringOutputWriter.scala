package org.canve.shared.Execution

import java.io.{PrintWriter, BufferedWriter, OutputStreamWriter, FileOutputStream, Closeable, Flushable}
import org.fusesource.jansi.AnsiOutputStream
import scala.sys.process._
import java.io.File
import org.canve.shared.PrintUtil._

/*
 * Takes care of routing a process's stdout and stderr to a file, being a proper 
 * ProcessorLogger callback object for Scala's ProcessBuilder methods. Inspired by 
 * the original FileProcessorLogger in scala.sys.process.
 */

class FilteringOutputWriter(outFile: File, timeString: String, liftError: Boolean = false) 
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
  
  writer.println(wrap("Following is the stdout and stderr output of the process started on " + timeString))
  
  def out(s: ⇒ String): Unit = {
    writer.println(s)
    print(".")  
  }
  
  def err(s: ⇒ String): Unit = {
    writer.println("[stderr:] " + s)
    
    // avoids error indication for commands where stderr is used for information rather than errors
    liftError match {
      case false => print("<error>")
      case true => print(".")
    }
  }
  
  def buffer[T](f: => T): T = f
  
  def close(): Unit = writer.close()
  def flush(): Unit = writer.flush()
}