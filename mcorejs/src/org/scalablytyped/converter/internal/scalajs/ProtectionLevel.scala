package org.scalablytyped.converter.internal.scalajs

sealed trait ProtectionLevel

object ProtectionLevel {
  def stricter(one: ProtectionLevel, two: ProtectionLevel): ProtectionLevel =
    (one, two) match {
      case (p @ Private(_), _) => p
      case (_, p @ Private(_)) => p
      case (Protected, _)      => Protected
      case (_, Protected)      => Protected
      case _                   => Public
    }

  case class Private(within: Option[Name]) extends ProtectionLevel
  case object Protected extends ProtectionLevel
  case object Public extends ProtectionLevel
}
