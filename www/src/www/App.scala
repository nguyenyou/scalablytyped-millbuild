package www

import org.scalajs.dom
import com.raquo.laminar.api.L.*
import org.scalablytyped.converter.internal.ts.parser

case class App() {

  val dts =
    s"""|declare namespace clsx {
        |    type ClassValue = ClassArray | ClassDictionary | string | number | bigint | null | boolean | undefined;
        |    type ClassDictionary = Record<string, any>;
        |    type ClassArray = ClassValue[];
        |    function clsx(...inputs: ClassValue[]): string;
        |}
        |declare function clsx(...inputs: clsx.ClassValue[]): string;
        |export = clsx;
        """.stripMargin

  // Parse the TypeScript content using the mtsjs parser
  val astContent = try {
    val parseResult = parser.parseString(dts)
    parseResult match {
      case Right(parsedFile) =>
        s"Successfully parsed! Found ${parsedFile.members.length} top-level declarations:\n" +
        parsedFile.members.zipWithIndex.map { case (member, idx) =>
          s"${idx + 1}. ${member.getClass.getSimpleName}: ${member.toString.take(100)}..."
        }.mkString("\n")
      case Left(error) =>
        s"Parse error: $error"
    }
  } catch {
    case ex: Throwable =>
      s"Exception during parsing: ${ex.getClass.getSimpleName}: ${ex.getMessage}"
  }

  def apply() = {
    div(
      cls("w-screen h-screen p-4"),
      div(
        cls("mb-4"),
        h2(cls("text-xl font-bold mb-2"), "TypeScript Definition:"),
        pre(cls("bg-gray-100 p-2 rounded text-sm overflow-auto"), dts)
      ),
      div(
        h2(cls("text-xl font-bold mb-2"), "Parsed AST:"),
        pre(cls("bg-blue-50 p-2 rounded text-sm overflow-auto whitespace-pre-wrap"), astContent)
      )
    )
  }
}
