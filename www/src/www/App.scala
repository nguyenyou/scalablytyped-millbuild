package www

import org.scalajs.dom
import com.raquo.laminar.api.L.*
import org.scalablytyped.converter.internal.ts.parser
import www.components.LaminarComponent

case class App() extends LaminarComponent {

  // State management
  val codeVar = Var(CodeInput.Examples.simple)
  val parseResultVar = Var[Either[String, org.scalablytyped.converter.internal.ts.TsParsedFile]](Right(null))
  val isParsingVar = Var(false)

  // Parse function
  def parseCode(code: String): Unit = {
    if (code.trim.isEmpty) {
      parseResultVar.set(Right(null))
      return
    }

    isParsingVar.set(true)

    // Use a small delay to show parsing state
    dom.window.setTimeout(() => {
      try {
        val result = parser.parseString(code)
        parseResultVar.set(result)
      } catch {
        case ex: Throwable =>
          parseResultVar.set(Left(s"Exception during parsing: ${ex.getClass.getSimpleName}: ${ex.getMessage}"))
      } finally {
        isParsingVar.set(false)
      }
    }, 100)
  }

  // Initialize with default example
  parseCode(codeVar.now())

  def render() = {
    div(
      cls("w-screen h-screen flex flex-col bg-gray-50"),

      // Header
      headerTag(
        cls("bg-white border-b border-gray-200 px-6 py-4"),
        div(
          cls("flex items-center justify-between"),
          div(
            h1(cls("text-2xl font-bold text-gray-900"), "TypeScript AST Parser"),
            p(cls("text-gray-600 text-sm mt-1"), "Parse TypeScript declaration files and explore their Abstract Syntax Tree")
          ),
          div(
            cls("flex items-center gap-4"),
            div(
              cls("flex items-center gap-2"),
              div(
                cls("w-3 h-3 rounded-full"),
                cls.toggle("bg-green-400") <-- isParsingVar.signal.map(!_),
                cls.toggle("bg-yellow-400 animate-pulse") <-- isParsingVar.signal
              ),
              span(
                cls("text-sm text-gray-600"),
                child <-- isParsingVar.signal.map { isParsing =>
                  if (isParsing) span("Parsing...") else span("Ready")
                }
              )
            )
          )
        )
      ),

      // Main content
      div(
        cls("flex-1 flex overflow-hidden mobile-stack"),

        // Left panel - Input
        div(
          cls("w-1/2 flex flex-col border-r border-gray-200 bg-white mobile-full panel-shadow"),

          // Input header
          div(
            cls("px-6 py-4 border-b border-gray-200 bg-gray-50"),
            h2(cls("text-lg font-semibold text-gray-800"), "TypeScript Code"),
            p(cls("text-sm text-gray-600 mt-1"), "Enter TypeScript declaration code to parse")
          ),

          // Example selector
          div(
            cls("px-6 py-3 border-b border-gray-200"),
            CodeInput.exampleSelector { example =>
              codeVar.set(example)
              parseCode(example)
            }
          ),

          // Code input
          div(
            cls("flex-1 p-6 overflow-hidden"),
            CodeInput(CodeInput.Props(
              value = codeVar.now(),
              onChange = { newCode =>
                codeVar.set(newCode)
                parseCode(newCode)
              },
              disabled = false
            ))()
          )
        ),

        // Right panel - Output
        div(
          cls("w-1/2 flex flex-col bg-white mobile-full panel-shadow"),

          // Error display
          div(
            cls("px-6 py-4"),
            child <-- parseResultVar.signal.map { result =>
              ErrorDisplay(ErrorDisplay.Props(
                error = ErrorDisplay.fromParseResult(result, codeVar.now()),
                sourceCode = codeVar.now()
              ))()
            }
          ),

          // AST display
          div(
            cls("flex-1 overflow-hidden"),
            child <-- parseResultVar.signal.map { result =>
              result match {
                case Right(parsedFile) if parsedFile != null =>
                  AstDisplay(AstDisplay.Props(
                    parsedFile = Some(parsedFile),
                    viewMode = ViewMode.Tree
                  ))()
                case Right(_) =>
                  div(
                    cls("flex items-center justify-center h-full text-gray-500"),
                    div(
                      cls("text-center"),
                      div(cls("text-4xl mb-4"), "📝"),
                      div(cls("text-lg"), "Enter TypeScript code"),
                      div(cls("text-sm"), "Start typing or select an example to see the AST")
                    )
                  )
                case Left(_) =>
                  div(
                    cls("flex items-center justify-center h-full text-gray-500"),
                    div(
                      cls("text-center"),
                      div(cls("text-4xl mb-4"), "❌"),
                      div(cls("text-lg"), "Parse Error"),
                      div(cls("text-sm"), "Fix the syntax errors to see the AST")
                    )
                  )
              }
            }
          )
        )
      )
    )
  }
}
