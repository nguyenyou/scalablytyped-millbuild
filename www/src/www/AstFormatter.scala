package www

import org.scalablytyped.converter.internal.ts._

object AstFormatter {
  
  case class TreeNode(
    nodeType: String,
    name: String,
    details: String,
    children: List[TreeNode]
  )
  
  def formatAsTree(parsedFile: TsParsedFile): TreeNode = {
    TreeNode(
      nodeType = "TsParsedFile",
      name = "Root",
      details = s"${parsedFile.members.length} members, ${parsedFile.directives.length} directives",
      children = parsedFile.members.toList.map(formatContainerOrDecl)
    )
  }
  
  private def formatContainerOrDecl(node: TsContainerOrDecl): TreeNode = {
    node match {
      case decl: TsNamedDecl => formatNamedDecl(decl)
      case container: TsContainer => formatContainer(container)
      case other => TreeNode(
        nodeType = other.getClass.getSimpleName,
        name = "",
        details = other.toString.take(100),
        children = List.empty
      )
    }
  }
  
  private def formatNamedDecl(decl: TsNamedDecl): TreeNode = {
    val baseNode = TreeNode(
      nodeType = decl.getClass.getSimpleName,
      name = decl.name.value,
      details = "",
      children = List.empty
    )
    
    decl match {
      case iface: TsDeclInterface =>
        baseNode.copy(
          details = s"Interface with ${iface.members.length} members",
          children = iface.members.toList.map(formatMember)
        )
      case cls: TsDeclClass =>
        baseNode.copy(
          details = s"Class with ${cls.members.length} members" +
                   (if (cls.isAbstract) " (abstract)" else "") +
                   (if (cls.declared) " (declared)" else ""),
          children = cls.members.toList.map(formatMember)
        )
      case ns: TsDeclNamespace =>
        baseNode.copy(
          details = s"Namespace with ${ns.members.length} members",
          children = ns.members.toList.map(formatContainerOrDecl)
        )
      case mod: TsDeclModule =>
        baseNode.copy(
          details = s"Module with ${mod.members.length} members",
          children = mod.members.toList.map(formatContainerOrDecl)
        )
      case enumDecl: TsDeclEnum =>
        baseNode.copy(
          details = s"Enum with ${enumDecl.members.length} members",
          children = enumDecl.members.toList.map(formatEnumMember)
        )
      case func: TsDeclFunction =>
        baseNode.copy(
          details = s"Function${formatFunctionSignature(func.signature)}",
          children = List.empty
        )
      case variable: TsDeclVar =>
        baseNode.copy(
          details = s"Variable: ${formatType(variable.tpe.getOrElse(TsTypeRef.any))}",
          children = List.empty
        )
      case typeAlias: TsDeclTypeAlias =>
        baseNode.copy(
          details = s"Type alias: ${formatType(typeAlias.alias)}",
          children = List.empty
        )
      case other =>
        baseNode.copy(details = other.toString.take(100))
    }
  }
  
  private def formatContainer(container: TsContainer): TreeNode = {
    TreeNode(
      nodeType = container.getClass.getSimpleName,
      name = "",
      details = s"Container with ${container.members.length} members",
      children = container.members.toList.map(formatContainerOrDecl)
    )
  }
  
  private def formatMember(member: TsMember): TreeNode = {
    val baseNode = TreeNode(
      nodeType = member.getClass.getSimpleName,
      name = "",
      details = "",
      children = List.empty
    )
    
    member match {
      case prop: TsMemberProperty =>
        baseNode.copy(
          name = prop.name.value,
          details = s"Property: ${formatType(prop.tpe.getOrElse(TsTypeRef.any))}" +
                   (if (prop.isReadOnly) " (readonly)" else "")
        )
      case func: TsMemberFunction =>
        baseNode.copy(
          name = func.name.value,
          details = s"Method${formatFunctionSignature(func.signature)}"
        )
      case call: TsMemberCall =>
        baseNode.copy(
          name = "call",
          details = s"Call signature${formatFunctionSignature(call.signature)}"
        )
      case ctor: TsMemberCtor =>
        baseNode.copy(
          name = "constructor",
          details = s"Constructor${formatFunctionSignature(ctor.signature)}"
        )
      case index: TsMemberIndex =>
        val indexDetails = index.indexing match {
          case Indexing.Dict(name, tpe) => s"[${name.value}: ${formatType(tpe)}]"
          case Indexing.Single(name) => s"[${name.parts.map(_.value).mkString(".")}]"
        }
        baseNode.copy(
          name = "index",
          details = s"Index signature: $indexDetails: ${formatType(index.valueType.getOrElse(TsTypeRef.any))}"
        )
      case other =>
        baseNode.copy(details = other.toString.take(100))
    }
  }
  
  private def formatEnumMember(member: TsEnumMember): TreeNode = {
    TreeNode(
      nodeType = "TsEnumMember",
      name = member.name.value,
      details = member.expr.map(_.toString).getOrElse(""),
      children = List.empty
    )
  }
  
  private def formatFunctionSignature(sig: TsFunSig): String = {
    val typeParams = if (sig.tparams.nonEmpty) {
      s"<${sig.tparams.map(_.name.value).mkString(", ")}>"
    } else ""
    
    val params = sig.params.map { param =>
      s"${param.name.value}: ${formatType(param.tpe.getOrElse(TsTypeRef.any))}"
    }.mkString(", ")
    
    val returnType = sig.resultType.map(formatType).getOrElse("void")
    
    s"$typeParams($params): $returnType"
  }
  
  private def formatType(tpe: TsType): String = {
    tpe match {
      case TsTypeRef(_, name, targs) =>
        val base = name.parts.map(_.value).mkString(".")
        if (targs.nonEmpty) {
          s"$base<${targs.map(formatType).mkString(", ")}>"
        } else base
      case TsTypeLiteral(lit) => lit.literal
      case TsTypeObject(_, members) => s"{${members.length} members}"
      case TsTypeFunction(sig) => s"${formatFunctionSignature(sig)}"
      case TsTypeUnion(types) => types.map(formatType).mkString(" | ")
      case TsTypeIntersect(types) => types.map(formatType).mkString(" & ")
      case TsTypeTuple(elems) => s"[${elems.map(elem => formatType(elem.tpe)).mkString(", ")}]"
      case other => other.getClass.getSimpleName
    }
  }
  
  def renderTreeAsText(node: TreeNode, indent: Int = 0): String = {
    val prefix = "  " * indent
    val nodeInfo = if (node.name.nonEmpty) {
      s"${node.nodeType}(${node.name})"
    } else {
      node.nodeType
    }
    
    val details = if (node.details.nonEmpty) s" - ${node.details}" else ""
    val currentLine = s"$prefix$nodeInfo$details"
    
    val childrenText = node.children.map(child => renderTreeAsText(child, indent + 1)).mkString("\n")
    
    if (childrenText.nonEmpty) {
      s"$currentLine\n$childrenText"
    } else {
      currentLine
    }
  }
}
