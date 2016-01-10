package org.canve
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import SbtOwnBuildInfo.info._
import org.canve.shared.ReadyOutDir

package object githubCruncher {
  val outDirectory = ReadyOutDir(baseDirectory.toString + File.separator + "out")
}