package org.scalablytyped.converter.internal
package ts

package object parser {
  val BOM = "\uFEFF"

  def cleanedString(s1: String): String = {
    val s2 = if (s1.startsWith(BOM)) s1.replace(BOM, "") else s1
    val s3 = s2.replace("\r\n", "\n").trim
    s3
  }

  /** ScalaJS-compatible parser function that takes a string and returns a
    * parsed AST This version doesn't depend on file system operations
    */
  def parseString(
      content: String,
      fileName: String = "input.d.ts"
  ): Either[String, TsParsedFile] = {
    val str = cleanedString(content)
    val p = new TsParser(None) // No file path dependency for ScalaJS

    p.phrase(p.parsedTsFile)(new TsParser.lexical.Scanner(str)) match {
      case p.Success(t, _) =>
        Right(t)

      case p.NoSuccess(msg, next) =>
        Left(s"Parse error at ${next.pos} $msg")
    }
  }

  // Keep original functions for compatibility but mark them as unavailable in ScalaJS
  @deprecated("Use parseString for ScalaJS compatibility", "1.0")
  def parseFile(inFile: InFile): Either[String, TsParsedFile] =
    throw new UnsupportedOperationException(
      "File operations not supported in ScalaJS"
    )

  @deprecated("Use parseString for ScalaJS compatibility", "1.0")
  def parseFileContent(
      inFile: InFile,
      bytes: Array[Byte]
  ): Either[String, TsParsedFile] =
    throw new UnsupportedOperationException(
      "File operations not supported in ScalaJS"
    )
}
