package org.scalablytyped.converter.internal
package ts

// Simplified JsLocation for ScalaJS compatibility
sealed trait JsLocation

object JsLocation {
  case object Zero extends JsLocation
  
  trait Has {
    def jsLocation: JsLocation
    def withJsLocation(newLocation: JsLocation): Has
  }
}
