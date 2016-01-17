package org.canve.shared
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import com.sun.management.UnixOperatingSystemMXBean


object IO {
  def getSubDirectories(dir: String): List[File] = {
    val directory = new File(dir) 
    if (!directory.exists) throw new Exception(s"API usage error: cannot invoke this function on a non-existant directory ($dir)") // otherwise ugly java exception 
    directory.listFiles.toList.filter(_.isDirectory())
  }
  
  def getOpenFilesCount = {    
    val os = ManagementFactory.getOperatingSystemMXBean
      if(!os.isInstanceOf[UnixOperatingSystemMXBean]) throw new Exception("method called only supports Unix")
      os.asInstanceOf[UnixOperatingSystemMXBean].getOpenFileDescriptorCount
  }
}