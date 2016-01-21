package org.canve.shared
import java.io.File

/*
 * Represents a directory holding:
 * 
 * - a directory for data
 * - a directory for logs of the data's creation
 */
class DataWithLog(val outputRootDir: String) {
  val dataDir = ReadyOutDir(outputRootDir + File.separator + "data")
  val logDir = ReadyOutDir(outputRootDir + File.separator + "creation-log")
  val base = outputRootDir  
}