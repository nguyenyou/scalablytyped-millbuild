package org.scalablytyped.converter.internal

sealed trait Comment

/* We need a few pieces of out of band information bundled within the tree,
  to be used like annotations. Instead of actually inventing annotations on
  the typescript side we rather just work with special comments.
 */
sealed trait Marker extends Comment

object Marker {
  case object CouldBeScalaJsDefined extends Marker
  case object IsTrivial extends Marker
  case object ExpandedCallables extends Marker
  case object ExpandedClass extends Marker
  case object EnumObject extends Marker
  case object HasClassParent extends Marker

  case class NameHint(value: String) extends Marker
  case object ManglerLeaveAlone extends Marker
  case object ManglerWasJsNative extends Marker
}

object Comment {
  final case class Raw(raw: String) extends Comment

  def apply(raw: String): Comment = Comment.Raw(raw)

  def warning(s: String): Comment =
    Comment(s"/* import warning: $s */")
}
