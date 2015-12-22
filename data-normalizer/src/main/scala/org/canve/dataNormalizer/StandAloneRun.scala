package org.canve.dataNormalizer

object StandAloneRun extends App {
  val path = args.length match {
    case len: Int if len > 1  => throw new Exception("Only one argument is allowed, doing nothing.") 
    case len: Int if len == 0 => throw new Exception("No argument supplied, doing nothing. Need to supply a path to a canve-dir")
    case len: Int if len == 1 =>
      args.head
  }
  
  val normalizedData = org.canve.compilerPlugin.normalization.CrossProjectNormalizer.normalize(Some(path))
}