package org.scalablytyped.converter.internal
package ts

sealed trait TsExpr

object TsExpr {
  case class Ref(value: TsQIdent) extends TsExpr
  case class Literal(value: TsLiteral) extends TsExpr
}
