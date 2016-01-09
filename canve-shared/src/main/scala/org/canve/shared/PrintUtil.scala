package org.canve.shared

object PrintUtil {
  def wrap(text: String, char: Char = '-'): String = {
    val wrap = char.toString * text.length
    s"$wrap\n$text\n$wrap"
  }
}