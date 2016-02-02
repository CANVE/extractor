package org.canve.compilerPlugin
import scala.tools.nsc.Global

class TypeTreeHead extends Type

trait Type { 
  /* the lower bound for this type, if any */
  val lowerBound: Option[Type] = ???
  
  /* the higher bound for this type, if any */
  val upperBound: Option[Type] = ???
}

class NonStructuralType (  
  /* the symbol defining this type */
  val typeSymbol: Symbol, 
  
  /* type arguments being provided for this type */
  val typeArgs: Seq[Type],
  
  /* if this type has a self type which is not itself (i.e. an explicit self type of a trait), then that type */
  val selfType: Option[Type]
) extends Type

class StructuralType(structuralType: Option[String]) extends Type