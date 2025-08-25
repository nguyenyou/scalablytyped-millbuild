package org.scalablytyped.converter.internal
package ts

object TsTypeFormatter extends TsTypeFormatter(true)

class TsTypeFormatter(val keepComments: Boolean) {
  def dropComments = new TsTypeFormatter(false)

  def qident(q: TsQIdent): String =
    q.parts.map(_.value).mkString(".")

  def sig(sig: TsFunSig): String =
    List[Option[String]](
      tparams(sig.tparams)(tparam),
      Some(sig.params.map(param).mkString("(", ", ", ")")),
      sig.resultType.map(apply).map(": " + _)
    ).flatten.mkString("")

  def tparam(tparam: TsTypeParam): String =
    tparam match {
      case TsTypeParam(name, bound, default, _) =>
        List[Option[String]](
          Some(name.value),
          bound.map(b => s"extends ${apply(b)}"),
          default.map(d => s"= " + apply(d))
        ).flatten.mkString(" ")
    }

  def param(p: TsFunParam): String =
    p match {
      case TsFunParam(_, name, tpe) =>
        List[Option[String]](
          Some(name.value),
          tpe.map(x => s": ${apply(x)}")
        ).flatten.mkString(" ")
    }

  def tparams[T <: AnyRef](ts: IArray[T])(f: T => String): Option[String] =
    if (ts.isEmpty) None else Some("<" + ts.map(f).mkString(", ") + ">")

  def level(l: ProtectionLevel): Option[String] =
    l match {
      case ProtectionLevel.Default   => None
      case ProtectionLevel.Private   => Some("private")
      case ProtectionLevel.Protected => Some("protected")
    }

  def member(m: TsMember): String = m match {
    case TsMemberFunction(_, l, name, methodType, s, _, _) =>
      List[Option[String]](
        level(l),
        methodType match {
          case MethodType.Normal => None
          case MethodType.Getter => Some("get")
          case MethodType.Setter => Some("set")
        },
        Some(name.value),
        Some(sig(s))
      ).flatten.mkString(" ")

    case TsMemberProperty(_, l, name, tpe, _, _, _) =>
      List[Option[String]](
        level(l),
        Some(name.value),
        tpe.map(apply).map(":" + _)
      ).flatten.mkString(" ")

    case TsMemberCall(_, l, s) =>
      s"${level(l).getOrElse("")} ${sig(s)}"

    case TsMemberCtor(_, _, s) =>
      s"new ${sig(s)}"
  }

  def lit(lit: TsLiteral): String = lit match {
    case TsLiteralString(str)   => s"'$str'"
    case TsLiteralBoolean(bool) => bool
    case TsLiteralNumber(num)   => num
  }

  def apply(tpe: TsType): String =
    tpe match {
      case TsTypeRef(cs, name, ts) =>
        Comments.format(cs, keepComments) + qident(name) + tparams(ts)(apply)
          .getOrElse("")
      case TsTypeLiteral(l) => lit(l)
      case TsTypeObject(_, members) =>
        s"{${members.map(member).mkString(", ")}}"
      case TsTypeFunction(s)      => s"${sig(s)}"
      case TsTypeUnion(types)     => types.map(apply).mkString(" | ")
      case TsTypeIntersect(types) => types.map(apply).mkString(" & ")
      case TsTypeConditional(pred, ifTrue, ifFalse) =>
        s"${apply(pred)} ? ${apply(ifTrue)} : ${apply(ifFalse)}"
      case TsTypeInfer(tparam) =>
        s"infer ${tparam.name.value}"
    }
}
