package www

import org.scalajs.dom
import com.raquo.laminar.api.L.*
import www.components.LaminarComponent
import ErrorDisplay.ParseError

case class ErrorDisplay(props: ErrorDisplay.Props) extends LaminarComponent {
   
  def render(): HtmlElement = {
    props.error match {
      case Some(error) => renderError(error, props.sourceCode)
      case None => div() // Return empty div instead of emptyNode
    }
  }
  
  private def renderError(error: ParseError, sourceCode: String): HtmlElement = {
    div(
      cls("bg-red-50 border border-red-200 rounded-lg p-4 mb-4"),
      
      // Error header
      div(
        cls("flex items-center mb-3"),
        span(
          cls("inline-flex items-center justify-center w-8 h-8 bg-red-100 text-red-600 rounded-full mr-3"),
          "⚠"
        ),
        div(
          h4(
            cls("text-red-800 font-semibold text-lg"),
            "Parse Error"
          ),
          error.line.map { line =>
            p(
              cls("text-red-600 text-sm"),
              s"Line ${line}${error.column.map(col => s", Column $col").getOrElse("")}"
            )
          }.getOrElse(emptyNode)
        )
      ),
      
      // Error message
      div(
        cls("bg-white border border-red-200 rounded p-3 mb-3"),
        p(
          cls("text-red-800 font-mono text-sm"),
          error.message
        )
      ),
      
      // Source context if available
      error.line.map { lineNum =>
        renderSourceContext(sourceCode, lineNum, error.column)
      }.getOrElse(emptyNode),
      
      // Help section
      renderHelpSection(error)
    )
  }
  
  private def renderSourceContext(sourceCode: String, errorLine: Int, errorColumn: Option[Int]) = {
    val lines = sourceCode.split('\n')
    val contextRadius = 2
    val startLine = math.max(0, errorLine - contextRadius - 1)
    val endLine = math.min(lines.length - 1, errorLine + contextRadius - 1)
    
    if (startLine <= endLine && errorLine > 0 && errorLine <= lines.length) {
      div(
        cls("mb-3"),
        h5(
          cls("text-red-700 font-medium mb-2 text-sm"),
          "Source Context:"
        ),
        div(
          cls("bg-gray-900 text-gray-100 rounded p-3 font-mono text-sm overflow-x-auto"),
          (startLine to endLine).map { lineIndex =>
            val lineNumber = lineIndex + 1
            val lineContent = if (lineIndex < lines.length) lines(lineIndex) else ""
            val isErrorLine = lineNumber == errorLine
            
            div(
              cls("flex"),
              cls.toggle("bg-red-900 bg-opacity-50") := isErrorLine,
              
              // Line number
              span(
                cls("text-gray-400 mr-4 select-none w-8 text-right"),
                cls.toggle("text-red-300 font-bold") := isErrorLine,
                lineNumber.toString
              ),
              
              // Line content
              span(
                cls("flex-1"),
                if (lineContent.trim.isEmpty) " " else lineContent
              )
            )
          },
          
          // Error pointer if column is available
          errorColumn.map { col =>
            div(
              cls("flex text-red-400"),
              span(cls("w-8 mr-4")), // Space for line number
              span(
                cls("font-bold"),
                " " * (col - 1) + "^"
              )
            )
          }.getOrElse(emptyNode)
        )
      )
    } else emptyNode
  }
  
  private def renderHelpSection(error: ParseError) = {
    val suggestions = generateSuggestions(error.message)
    
    if (suggestions.nonEmpty) {
      div(
        cls("bg-blue-50 border border-blue-200 rounded p-3"),
        h5(
          cls("text-blue-800 font-medium mb-2 text-sm"),
          "💡 Suggestions:"
        ),
        ul(
          cls("text-blue-700 text-sm space-y-1"),
          suggestions.map { suggestion =>
            li(
              cls("flex items-start"),
              span(cls("mr-2"), "•"),
              span(suggestion)
            )
          }
        )
      )
    } else emptyNode
  }
  
  private def generateSuggestions(errorMessage: String): List[String] = {
    val message = errorMessage.toLowerCase
    
    val suggestions = List(
      ("expected", List(
        "Check for missing semicolons, commas, or brackets",
        "Verify that all opening brackets have matching closing brackets",
        "Make sure interface and type declarations are properly formatted"
      )),
      ("unexpected", List(
        "Look for extra characters or symbols",
        "Check if you're using reserved keywords incorrectly",
        "Verify the syntax matches TypeScript declaration file format"
      )),
      ("identifier", List(
        "Ensure identifiers start with a letter, underscore, or dollar sign",
        "Check that you're not using reserved keywords as identifiers",
        "Verify proper naming conventions for types and interfaces"
      )),
      ("string", List(
        "Make sure string literals are properly quoted",
        "Check for unescaped quotes within strings",
        "Verify template literal syntax if using backticks"
      )),
      ("type", List(
        "Check type annotations and declarations",
        "Verify generic type parameter syntax",
        "Make sure union and intersection types use correct operators (| and &)"
      )),
      ("function", List(
        "Check function signature syntax",
        "Verify parameter and return type annotations",
        "Make sure function overloads are properly declared"
      )),
      ("interface", List(
        "Check interface declaration syntax",
        "Verify property and method signatures",
        "Make sure extends clauses are properly formatted"
      )),
      ("namespace", List(
        "Check namespace declaration syntax",
        "Verify nested declarations within namespaces",
        "Make sure export statements are correctly placed"
      ))
    )
    
    suggestions.find { case (keyword, _) => message.contains(keyword) }
      .map(_._2)
      .getOrElse(List(
        "Check the TypeScript handbook for declaration file syntax",
        "Verify that your code follows TypeScript declaration file conventions",
        "Try simplifying the code to isolate the syntax error"
      ))
  }
  
}

object ErrorDisplay {
   
  case class ParseError(
    message: String,
    line: Option[Int] = None,
    column: Option[Int] = None,
    context: Option[String] = None
  )
  case class Props(
    error: Option[ParseError],
    sourceCode: String = ""
  )


  def fromParseResult(result: Either[String, ?], sourceCode: String = ""): Option[ParseError] = {
    result match {
      case Left(errorMsg) => Some(parseErrorMessage(errorMsg, sourceCode))
      case Right(_) => None
    }
  }
  
  private def parseErrorMessage(errorMsg: String, sourceCode: String): ParseError = {
    // Try to extract line and column information from error message
    val lineColumnRegex = """.*at\s+(\d+):(\d+).*""".r
    val lineRegex = """.*line\s+(\d+).*""".r
    val positionRegex = """.*position\s+(\d+).*""".r
    
    errorMsg match {
      case lineColumnRegex(lineStr, colStr) =>
        ParseError(
          message = errorMsg,
          line = Some(lineStr.toInt),
          column = Some(colStr.toInt)
        )
      case lineRegex(lineStr) =>
        ParseError(
          message = errorMsg,
          line = Some(lineStr.toInt)
        )
      case positionRegex(posStr) =>
        // Convert position to line/column
        val position = posStr.toInt
        val lines = sourceCode.split('\n')
        var currentPos = 0
        var lineNum = 1
        var colNum = 1
        
        for (line <- lines if currentPos < position) {
          if (currentPos + line.length + 1 > position) {
            colNum = position - currentPos + 1
          } else {
            currentPos += line.length + 1 // +1 for newline
            lineNum += 1
            colNum = 1
          }
        }
        
        ParseError(
          message = errorMsg,
          line = Some(lineNum),
          column = Some(colNum)
        )
      case _ =>
        ParseError(message = errorMsg)
    }
  }
}