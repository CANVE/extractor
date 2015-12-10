package org.canve.scalaPlus

trait CollectionPlus {
  def pairs[T](l: List[T]) = l.combinations(2).filter(pair => pair.head != pair.last)
}