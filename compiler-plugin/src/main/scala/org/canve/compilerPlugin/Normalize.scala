package org.canve.compilerPlugin
import org.canve.util.CanveDataIO._
import com.github.tototoshi.csv._
import java.io.File

class Normalize {
  
    getSubDirectories(canveRoot) map { dir =>
    println(s"reading nodes data from $dir")
    val Nodes = CSVReader.open(new File(dir + File.separator + "nodes")).allWithHeaders.map(rowMap => 
      Node(
        rowMap("id").toInt,
        rowMap("name"),
        rowMap("kind"),
        rowMap("notSynthetic").toBoolean,
        Some(rowMap("definition")),
        None))
  }

  
}