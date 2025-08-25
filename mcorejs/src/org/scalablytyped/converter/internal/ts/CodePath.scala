package org.scalablytyped.converter.internal
package ts

// Simplified CodePath for ScalaJS compatibility
sealed trait CodePath

object CodePath {
  case object NoPath extends CodePath
  
  trait Has {
    def codePath: CodePath
    def withCodePath(newCodePath: CodePath): Has
  }
}
