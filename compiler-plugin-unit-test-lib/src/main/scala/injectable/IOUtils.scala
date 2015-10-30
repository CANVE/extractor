package compilerPluginUnitTest

import java.io._

import scala.collection.{Set, mutable}
import scala.io.Source

/** @author Stephen Samuel */
object IOUtils {

  def getTempDirectory: File = new File(getTempPath)
  def getTempPath: String = System.getProperty("java.io.tmpdir")

  def readStreamAsString(in: InputStream): String = Source.fromInputStream(in).mkString

  private val UnixSeperator: Char = '/'
  private val WindowsSeperator: Char = '\\'
  private val UTF8Encoding: String = "UTF-8"

  def getName(path: String): Any = {
    val index = {
      val lastUnixPos = path.lastIndexOf(UnixSeperator)
      val lastWindowsPos = path.lastIndexOf(WindowsSeperator)
      Math.max(lastUnixPos, lastWindowsPos)
    }
    path.drop(index + 1)
  }

  def writeToFile(file: File, str: String) = {
    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UTF8Encoding))
    try {
      writer.write(str)
    } finally {
      writer.close()
    }
  }

  // loads all the invoked statement ids from the given files
  def invoked(files: Seq[File]): Set[Int] = {
    val acc = mutable.Set[Int]()
    files.foreach { file =>
      val reader = Source.fromFile(file)
      for ( line <- reader.getLines() ) {
        if (!line.isEmpty) {
          acc += line.toInt
        }
      }
      reader.close()
    }
    acc
  }

}
