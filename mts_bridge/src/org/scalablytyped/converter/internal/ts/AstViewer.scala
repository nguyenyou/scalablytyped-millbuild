package org.scalablytyped.converter.internal.ts

import org.scalablytyped.converter.internal.ts.parser._
import org.scalablytyped.converter.internal._
import scala.scalajs.js
import scala.scalajs.js.annotation._

/** ScalaJS-compatible TypeScript AST viewer API Provides methods to parse
  * TypeScript and return structured AST data
  */
@JSExportTopLevel("TsAstViewer")
object AstViewer {

  /** Parse TypeScript content and return a simplified AST representation
    * @param content
    *   TypeScript source code as string
    * @return
    *   Either error message or parsed AST data
    */
  @JSExport
  def parseTypeScript(content: String): js.Any = {
    parser.parseString(content) match {
      case Left(error) =>
        js.Dynamic.literal(
          "success" -> false,
          "error" -> error
        )
      case Right(parsed) =>
        js.Dynamic.literal(
          "success" -> true,
          "ast" -> convertToSimpleJsObject(parsed)
        )
    }
  }

  /** Convert TsParsedFile to a simple JavaScript-friendly object structure
    */
  private def convertToSimpleJsObject(parsed: TsParsedFile): js.Any = {
    js.Dynamic.literal(
      "type" -> "TsParsedFile",
      "memberCount" -> parsed.members.length,
      "members" -> convertMembersSimple(parsed.members),
      "isStdLib" -> parsed.isStdLib
    )
  }

  private def convertMembersSimple(
      members: IArray[TsContainerOrDecl]
  ): js.Array[js.Any] = {
    val result = new js.Array[js.Any]()
    var i = 0
    while (i < members.length) {
      val member = members(i)
      val jsObj = js.Dynamic
        .literal(
          "type" -> member.getClass.getSimpleName,
          "toString" -> member.toString
        )
        .asInstanceOf[js.Any]
      result.push(jsObj)
      i += 1
    }
    result
  }
}
