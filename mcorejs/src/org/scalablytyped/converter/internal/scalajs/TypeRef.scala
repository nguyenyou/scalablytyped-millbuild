package org.scalablytyped.converter.internal
package scalajs

import io.circe.{Decoder, Encoder}

final case class TypeRef(
    typeName: QualifiedName,
    targs: IArray[TypeRef],
    comments: Comments
) {
  val name: Name = typeName.parts.last

  def withOptional(optional: Boolean): TypeRef =
    if (optional) TypeRef.UndefOr(this) else this

  def withComments(cs: Comments): TypeRef =
    if (cs.cs.isEmpty) this else TypeRef(typeName, targs, comments ++ cs)
}

object TypeRef {
  implicit val suffix: ToSuffix[TypeRef] = t => ToSuffix(t.typeName) ++ t.targs
  implicit lazy val encodes: Encoder[TypeRef] =
    io.circe.generic.semiauto.deriveEncoder
  implicit lazy val decodes: Decoder[TypeRef] =
    io.circe.generic.semiauto.deriveDecoder

  def apply(n: Name): TypeRef =
    TypeRef(QualifiedName(IArray(n)), Empty, NoComments)
  def apply(qn: QualifiedName): TypeRef =
    TypeRef(qn, Empty, NoComments)

  def stripTargs(tr: TypeRef): TypeRef =
    tr.copy(targs = tr.targs.map(_ => TypeRef.Any))

  val Wildcard = TypeRef(QualifiedName.WILDCARD, Empty, NoComments)
  val Any = TypeRef(QualifiedName.Any, Empty, NoComments)
  val AnyRef = TypeRef(QualifiedName.AnyRef, Empty, NoComments)
  val AnyVal = TypeRef(QualifiedName.AnyVal, Empty, NoComments)
  val Boolean = TypeRef(QualifiedName.Boolean, Empty, NoComments)
  val Byte = TypeRef(QualifiedName.Byte, Empty, NoComments)
  val Char = TypeRef(QualifiedName.Char, Empty, NoComments)
  val Double = TypeRef(QualifiedName.Double, Empty, NoComments)
  val Float = TypeRef(QualifiedName.Float, Empty, NoComments)
  val Int = TypeRef(QualifiedName.Int, Empty, NoComments)
  val Long = TypeRef(QualifiedName.Long, Empty, NoComments)
  val Nothing = TypeRef(QualifiedName.Nothing, Empty, NoComments)
  val Null = TypeRef(QualifiedName.Null, Empty, NoComments)
  val Short = TypeRef(QualifiedName.Short, Empty, NoComments)
  val String = TypeRef(QualifiedName.String, Empty, NoComments)
  val Unit = TypeRef(QualifiedName.Unit, Empty, NoComments)

  val JsAny = TypeRef(QualifiedName.JsAny, Empty, NoComments)
  val JsBigInt = TypeRef(QualifiedName.JsBigInt, Empty, NoComments)
  val JsDynamic = TypeRef(QualifiedName.JsDynamic, Empty, NoComments)
  val JsFunctionBase = TypeRef(QualifiedName.JsFunction, Empty, NoComments)
  val JsObject = TypeRef(QualifiedName.JsObject, Empty, NoComments)
  val JsSymbol = TypeRef(QualifiedName.JsSymbol, Empty, NoComments)

  /* we represent `js.UndefOr` as this fake type ref inside a union type. Note that it can also appear on its own */
  val undefined = TypeRef(QualifiedName.UNDEFINED, Empty, NoComments)

  val Primitive =
    Set(Boolean, Byte, Double, Float, Int, Long, Nothing, Null, Short, Unit)

  def StringDictionary(typeParam: TypeRef, comments: Comments): TypeRef =
    TypeRef(QualifiedName.StringDictionary, IArray(typeParam), comments)

  def NumberDictionary(typeParam: TypeRef, comments: Comments): TypeRef =
    TypeRef(QualifiedName.NumberDictionary, IArray(typeParam), comments)

  object TopLevel {
    def apply(tr: TypeRef): TypeRef =
      TypeRef(QualifiedName.TopLevel, IArray(tr), NoComments)

    def unapply(tr: TypeRef): Option[TypeRef] =
      tr match {
        case TypeRef(
              QualifiedName.TopLevel,
              IArray.exactlyOne(typeParam),
              NoComments
            ) =>
          Some(typeParam)
        case _ => None
      }
  }

  object UndefOr {
    def apply(tr: TypeRef): TypeRef =
      TypeRef(QualifiedName.UndefOr, IArray(tr), NoComments)

    def unapply(tr: TypeRef): Option[TypeRef] =
      tr match {
        case TypeRef(
              QualifiedName.UndefOr,
              IArray.exactlyOne(typeParam),
              NoComments
            ) =>
          Some(typeParam)
        case _ => None
      }
  }

  object Union {
    def apply(types: IArray[TypeRef]): TypeRef =
      TypeRef(QualifiedName.UNION, types, NoComments)

    def unapply(tr: TypeRef): Option[IArray[TypeRef]] =
      tr match {
        case TypeRef(QualifiedName.UNION, types, NoComments) => Some(types)
        case _                                               => None
      }
  }

  object Intersection {
    def apply(types: IArray[TypeRef]): TypeRef =
      TypeRef(QualifiedName.INTERSECTION, types, NoComments)

    def unapply(tr: TypeRef): Option[IArray[TypeRef]] =
      tr match {
        case TypeRef(QualifiedName.INTERSECTION, types, NoComments) =>
          Some(types)
        case _ => None
      }
  }
}
