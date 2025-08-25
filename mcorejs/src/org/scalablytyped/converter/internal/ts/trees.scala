package org.scalablytyped.converter.internal
package ts

import scala.util.hashing.MurmurHash3.productHash

sealed trait TsTree extends Serializable with Product {
  override def canEqual(that: Any): Boolean = that.## == ##
  override lazy val hashCode: Int = productHash(this)

  lazy val asString: String = {
    val name = this match {
      case named: TsNamedDecl => named.name.value
      case _                  => ""
    }
    s"${getClass.getSimpleName}($name)"
  }
}

sealed trait TsContainerOrDecl extends TsTree

sealed trait TsDecl extends TsContainerOrDecl

sealed trait TsContainer
    extends TsContainerOrDecl
    with MemberCache
    with CodePath.Has {
  def members: IArray[TsContainerOrDecl]
  def withMembers(newMembers: IArray[TsContainerOrDecl]): TsContainer
}

sealed trait TsNamedDecl extends TsDecl with CodePath.Has {
  val comments: Comments
  def withComments(cs: Comments): TsNamedDecl
  final def addComment(c: Comment) = withComments(comments + c)

  def name: TsIdent
  def withName(name: TsIdentSimple): TsNamedDecl
}

sealed trait TsNamedValueDecl extends TsNamedDecl

final case class TsParsedFile(
    comments: Comments,
    directives: IArray[Directive],
    members: IArray[TsContainerOrDecl],
    codePath: CodePath = CodePath.NoPath
) extends TsContainer {

  lazy val isStdLib: Boolean =
    directives.exists {
      case Directive.NoStdLib => true
      case _                  => false
    }

  override def withMembers(
      newMembers: IArray[TsContainerOrDecl]
  ): TsParsedFile =
    copy(members = newMembers)

  override def withCodePath(newCodePath: CodePath): TsParsedFile =
    copy(codePath = newCodePath)
}

// Basic identifier types
sealed trait TsIdent {
  def value: String
}

object TsIdent {
  val namespaced = TsIdentSimple("__namespaced")
  val Destructured = TsIdentSimple("__destructured")
  val default = TsIdentSimple("default")

  def apply(str: String): TsIdentSimple = TsIdentSimple(str)
  def unapply(ident: TsIdent): Option[String] = Some(ident.value)
}

sealed trait TsIdentSimple extends TsIdent

final case class TsIdentSimpleImpl(value: String) extends TsIdentSimple

object TsIdentSimple {
  def apply(value: String): TsIdentSimple = TsIdentSimpleImpl(value)
  def unapply(ident: TsIdentSimple): Option[String] = Some(ident.value)
}

case class TsIdentModule(value: String) extends TsIdent

case class TsQIdent(parts: IArray[TsIdentSimple]) {
  def +(other: TsIdent): TsQIdent = other match {
    case simple: TsIdentSimple => TsQIdent(parts :+ simple)
    case _                     => this
  }
}

object TsQIdent {
  def of(ident: TsIdent): TsQIdent = ident match {
    case simple: TsIdentSimple => TsQIdent(IArray(simple))
    case _                     => TsQIdent(IArray.Empty)
  }
}

// Basic declaration types
case class TsDeclInterface(
    comments: Comments,
    declared: Boolean,
    name: TsIdent,
    tparams: IArray[TsTypeParam],
    inheritance: IArray[TsTypeRef],
    members: IArray[TsMember],
    codePath: CodePath = CodePath.NoPath
) extends TsNamedDecl {
  def withComments(cs: Comments): TsDeclInterface = copy(comments = cs)
  def withName(name: TsIdentSimple): TsDeclInterface = copy(name = name)
  def withCodePath(newCodePath: CodePath): TsDeclInterface =
    copy(codePath = newCodePath)
}

case class TsDeclClass(
    comments: Comments,
    declared: Boolean,
    isAbstract: Boolean,
    name: TsIdent,
    tparams: IArray[TsTypeParam],
    parent: Option[TsTypeRef],
    implements: IArray[TsTypeRef],
    members: IArray[TsMember],
    codePath: CodePath = CodePath.NoPath
) extends TsNamedDecl {
  def withComments(cs: Comments): TsDeclClass = copy(comments = cs)
  def withName(name: TsIdentSimple): TsDeclClass = copy(name = name)
  def withCodePath(newCodePath: CodePath): TsDeclClass =
    copy(codePath = newCodePath)
}

case class TsDeclTypeAlias(
    comments: Comments,
    declared: Boolean,
    name: TsIdent,
    tparams: IArray[TsTypeParam],
    alias: TsType,
    codePath: CodePath = CodePath.NoPath
) extends TsNamedDecl {
  def withComments(cs: Comments): TsDeclTypeAlias = copy(comments = cs)
  def withName(name: TsIdentSimple): TsDeclTypeAlias = copy(name = name)
  def withCodePath(newCodePath: CodePath): TsDeclTypeAlias =
    copy(codePath = newCodePath)
}

case class TsDeclFunction(
    comments: Comments,
    declared: Boolean,
    name: TsIdent,
    signature: TsFunSig,
    codePath: CodePath = CodePath.NoPath
) extends TsNamedDecl {
  def withComments(cs: Comments): TsDeclFunction = copy(comments = cs)
  def withName(name: TsIdentSimple): TsDeclFunction = copy(name = name)
  def withCodePath(newCodePath: CodePath): TsDeclFunction =
    copy(codePath = newCodePath)
}

case class TsDeclVar(
    comments: Comments,
    declared: Boolean,
    name: TsIdent,
    tpe: Option[TsType],
    codePath: CodePath = CodePath.NoPath
) extends TsNamedDecl {
  def withComments(cs: Comments): TsDeclVar = copy(comments = cs)
  def withName(name: TsIdentSimple): TsDeclVar = copy(name = name)
  def withCodePath(newCodePath: CodePath): TsDeclVar =
    copy(codePath = newCodePath)
}

case class TsDeclEnum(
    comments: Comments,
    declared: Boolean,
    isConst: Boolean,
    name: TsIdent,
    members: IArray[TsEnumMember],
    codePath: CodePath = CodePath.NoPath
) extends TsNamedDecl {
  def withComments(cs: Comments): TsDeclEnum = copy(comments = cs)
  def withName(name: TsIdentSimple): TsDeclEnum = copy(name = name)
  def withCodePath(newCodePath: CodePath): TsDeclEnum =
    copy(codePath = newCodePath)
}

case class TsDeclNamespace(
    comments: Comments,
    declared: Boolean,
    name: TsIdentSimple,
    members: IArray[TsContainerOrDecl],
    codePath: CodePath = CodePath.NoPath,
    jsLocation: JsLocation = JsLocation.Zero
) extends TsNamedDecl
    with TsContainer
    with JsLocation.Has {
  def withComments(cs: Comments): TsDeclNamespace = copy(comments = cs)
  def withName(name: TsIdentSimple): TsDeclNamespace = copy(name = name)
  def withMembers(newMembers: IArray[TsContainerOrDecl]): TsDeclNamespace =
    copy(members = newMembers)
  def withCodePath(newCodePath: CodePath): TsDeclNamespace =
    copy(codePath = newCodePath)
  def withJsLocation(newLocation: JsLocation): TsDeclNamespace =
    copy(jsLocation = newLocation)
}

case class TsDeclModule(
    comments: Comments,
    declared: Boolean,
    name: TsIdentModule,
    members: IArray[TsContainerOrDecl],
    codePath: CodePath = CodePath.NoPath,
    jsLocation: JsLocation = JsLocation.Zero
) extends TsNamedDecl
    with TsContainer
    with JsLocation.Has {
  def withComments(cs: Comments): TsDeclModule = copy(comments = cs)
  def withName(name: TsIdentSimple): TsDeclModule =
    copy(name = TsIdentModule(name.value))
  def withMembers(newMembers: IArray[TsContainerOrDecl]): TsDeclModule =
    copy(members = newMembers)
  def withCodePath(newCodePath: CodePath): TsDeclModule =
    copy(codePath = newCodePath)
  def withJsLocation(newLocation: JsLocation): TsDeclModule =
    copy(jsLocation = newLocation)
}

case class TsGlobal(
    declared: Boolean,
    members: IArray[TsContainerOrDecl],
    comments: Comments,
    codePath: CodePath = CodePath.NoPath
) extends TsContainer {
  def withMembers(newMembers: IArray[TsContainerOrDecl]): TsGlobal =
    copy(members = newMembers)
  def withCodePath(newCodePath: CodePath): TsGlobal =
    copy(codePath = newCodePath)
}

// Import/Export types
case class TsImport(
    typeOnly: Boolean,
    imported: IArray[TsImported],
    from: TsImportee
) extends TsContainerOrDecl

case class TsExport(
    typeOnly: Boolean,
    exported: TsExportee,
    comments: Comments
) extends TsContainerOrDecl

// Type system
sealed trait TsType

case class TsTypeRef(
    comments: Comments,
    name: TsQIdent,
    tparams: IArray[TsType]
) extends TsType

object TsTypeRef {
  val undefined = TsTypeRef(
    NoComments,
    TsQIdent(IArray(TsIdentSimple("undefined"))),
    IArray.Empty
  )
  val `null` = TsTypeRef(
    NoComments,
    TsQIdent(IArray(TsIdentSimple("null"))),
    IArray.Empty
  )
}

case class TsTypeLiteral(literal: TsLiteral) extends TsType

case class TsTypeObject(
    comments: Comments,
    members: IArray[TsMember]
) extends TsType

case class TsTypeFunction(signature: TsFunSig) extends TsType

case class TsTypeUnion(types: IArray[TsType]) extends TsType

object TsTypeUnion {
  def simplified(types: IArray[TsType]): TsType = {
    val builder = IArray.Builder.empty[TsType]
    types.foreach {
      case TsTypeUnion(nested) => nested.foreach(builder += _)
      case other               => builder += other
    }
    val flattened = builder.result()

    // Simple deduplication using toList
    val distinct = IArray.fromArray(flattened.toList.distinct.toArray[TsType])

    distinct.length match {
      case 0 => TsTypeRef.undefined // fallback
      case 1 => distinct(0)
      case _ => TsTypeUnion(distinct)
    }
  }
}

case class TsTypeIntersect(types: IArray[TsType]) extends TsType

// Function signatures and parameters
case class TsFunSig(
    comments: Comments,
    tparams: IArray[TsTypeParam],
    params: IArray[TsFunParam],
    resultType: Option[TsType]
)

case class TsFunParam(
    comments: Comments,
    name: TsIdent,
    tpe: Option[TsType]
)

case class TsTypeParam(
    name: TsIdent,
    upperBound: Option[TsType],
    default: Option[TsType],
    comments: Comments
)

// Members
sealed trait TsMember

case class TsMemberProperty(
    comments: Comments,
    level: ProtectionLevel,
    name: TsIdent,
    tpe: Option[TsType],
    optional: OptionalModifier,
    readonly: ReadonlyModifier,
    static: Boolean
) extends TsMember

case class TsMemberFunction(
    comments: Comments,
    level: ProtectionLevel,
    name: TsIdent,
    methodType: MethodType,
    signature: TsFunSig,
    optional: OptionalModifier,
    static: Boolean
) extends TsMember

case class TsEnumMember(
    name: TsIdent,
    expr: Option[TsExpr],
    comments: Comments
)

// Literals
sealed trait TsLiteral {
  def literal: String
}

case class TsLiteralString(literal: String) extends TsLiteral
case class TsLiteralNumber(literal: String) extends TsLiteral
case class TsLiteralBoolean(literal: String) extends TsLiteral

// Import/Export related types
sealed trait TsImported
object TsImported {
  case class Ident(ident: TsIdent) extends TsImported
  case class Star(asOpt: Option[TsIdent]) extends TsImported
  case class Destructured(idents: IArray[(TsIdent, Option[TsIdent])])
      extends TsImported
}

sealed trait TsImportee
object TsImportee {
  case class From(module: TsIdentModule) extends TsImportee
  case class Required(module: TsIdentModule) extends TsImportee
  case class Local(qident: TsQIdent) extends TsImportee
}

sealed trait TsExportee
object TsExportee {
  case class Star(asOpt: Option[TsIdent], from: TsIdentModule)
      extends TsExportee
  case class Names(
      idents: IArray[(TsQIdent, Option[TsIdent])],
      fromOpt: Option[TsIdentModule]
  ) extends TsExportee
  case class Tree(tree: TsContainerOrDecl) extends TsExportee
}
