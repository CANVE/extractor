package org.canve.shared
import java.io.File

/*
 * Represents a directory holding:
 * 
 * - a directory for data
 * - a directory for logs of the data's creation
 */
class DataWithLog(val outputRootDir: String) {
  private val dataBase = ReadyOutDir(outputRootDir + File.separator + "data")
  val dataDir = ReadyOutDir(dataBase + File.separator + "data")
  val metaDataDir = ReadyOutDir(dataBase + File.separator + "meta")
  val logDir = ReadyOutDir(outputRootDir + File.separator + "creation-log")
  val performanceDir = ReadyOutDir(dataBase + File.separator + "performance")
  val base = outputRootDir  
}