package org.scalablytyped.converter.internal

import io.circe.{Decoder, Encoder}
import scala.collection.mutable
import scala.reflect.ClassTag

@SerialVersionUID(
  8167323919307012581L
) // something about this class seems brittle
sealed class Comments(val cs: List[Comment]) extends Serializable {
  def rawCs = cs.collect { case Comment.Raw(raw) => raw }

  def extract[T](pf: PartialFunction[Marker, T]): Option[(T, Comments)] =
    cs.partition {
      case marker: Marker if pf.isDefinedAt(marker) => true
      case _                                        => false
    } match {
      case (Nil, _) => None
      case (some, rest) =>
        val extracted = some.collect {
          case marker: Marker if pf.isDefinedAt(marker) => pf(marker)
        }
        if (extracted.nonEmpty) Some((extracted.head, Comments(rest)))
        else None
    }

  def has[T <: Marker: ClassTag]: Boolean =
    cs.exists {
      case _: T => true
      case _    => false
    }

  override val hashCode: Int = 0

  override def equals(obj: Any): Boolean =
    obj.isInstanceOf[Comments]

  override def toString: String =
    s"Comments(${cs.size})"

  def isEmpty: Boolean =
    cs.isEmpty

  def nonEmpty: Boolean =
    cs.nonEmpty

  def ++(that: Comments): Comments = {
    val ret = (this.cs, that.cs) match {
      case (Nil, Nil) => NoComments
      case (_, Nil)   => this
      case (Nil, _)   => that
      case (l, r)     => Comments(l ++ r)
    }
    ret
  }
  def ++?(thatOpt: Option[Comments]): Comments =
    thatOpt match {
      case Some(that) => this ++ that
      case None       => this
    }

  def +(c: Comment): Comments =
    new Comments(cs :+ c)

  def +?(oc: Option[Comment]): Comments =
    oc match {
      case None    => this
      case Some(c) => this + c
    }
}

case object NoComments extends Comments(Nil) {
  override def toString: String = "NoComments"
}

object Comments {
  def apply(h: String, tail: String*): Comments =
    new Comments(Comment(h) +: tail.map(Comment.apply).toList)

  def apply(cs: List[Comment]): Comments =
    cs match {
      case Nil   => NoComments
      case other => new Comments(other)
    }

  def apply(oc: Option[Comment]): Comments =
    oc match {
      case Some(c) => apply(c)
      case None    => NoComments
    }

  def apply(c: Comment): Comments =
    new Comments(List(c))

  def flatten[T <: AnyRef](ts: IArray[T])(f: T => Comments): Comments = {
    val buf = mutable.ArrayBuffer.empty[Comment]
    ts.foreach(t => buf ++= f(t).cs)
    apply(buf.distinct.toList)
  }

  def unapply(c: Comments): Some[List[Comment]] = Some(c.cs)

  def format(comments: Comments): String =
    comments.rawCs.mkString("")

  def format(comments: Comments, keepComments: Boolean): String =
    if (keepComments) format(comments) else ""

  implicit val encodes: Encoder[Comments] =
    Encoder[List[Comment]].contramap(_.cs)
  implicit val decodes: Decoder[Comments] =
    Decoder[List[Comment]].map(Comments.apply)
}
