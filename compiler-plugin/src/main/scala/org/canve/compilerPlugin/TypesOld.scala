/* 
 * It's not worth it working with an object model for the relations, 
 * because a graph api is more versatile and flexible enough to add/change indexing
 * behind a getter facade. So this was just a thought drill about type relationships

package org.canve.compilerPlugin
import scala.tools.nsc.Global

class TypeTree extends Type

class Type {
  /* the lower bound for this type, if any */
  val lowerBound: Option[Type]
  
  /* the higher bound for this type, if any */
  val upperBound: Option[Type]  
}

class NonStructuralType extends Type {
  
  /* the symbol defining this type */
  val typeSymbol: Symbol 
  
  /* type arguments being provided for this type */
  val typeArgs: Seq[Type]
  
  /* if this type has a self type which is not itself (i.e. an explicit self type of a trait), then that type */
  val selfType: Option[Type]
}

class StructuralType extends Type {
  val structuralType: Option[String]
}

*/