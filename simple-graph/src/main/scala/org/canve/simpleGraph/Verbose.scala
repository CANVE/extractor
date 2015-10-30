package org.canve.simpleGraph


/*
 * a representation encouraging readable `filterFunc` implementations
 */
case class FilterFuncArguments[Vertex, Edge](direction: EdgeDirectionAllowed, edge: Edge, peer: Vertex)

abstract sealed class EdgeDirectionAllowed
object Ingress extends EdgeDirectionAllowed
object Egress  extends EdgeDirectionAllowed