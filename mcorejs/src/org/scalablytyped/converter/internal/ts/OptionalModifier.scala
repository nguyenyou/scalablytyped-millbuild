package org.scalablytyped.converter.internal
package ts

sealed trait OptionalModifier {
  def isOptional: Boolean
}

object OptionalModifier {
  case object No extends OptionalModifier {
    def isOptional: Boolean = false
  }
  case object Yes extends OptionalModifier {
    def isOptional: Boolean = true
  }
}
