package org.canve.logging.targets

import org.canve.Logging.AbstractTarget

/*
 * the console target
 */
class ConsoleTarget extends AbstractTarget {
  def apply(lines: List[String]) = 
    lines.map(l => println(Console.BLUE + Console.BOLD + "[canve] " + Console.RESET + l))
}
