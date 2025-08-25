package www

import org.scalajs.dom
import com.raquo.laminar.api.L.*
import org.scalablytyped.converter.internal.ts.TsParsedFile
import www.components.LaminarComponent

case class AstDisplay(props: AstDisplay.Props) extends LaminarComponent {
  def render(): HtmlElement = {
    val viewModeVar = Var(props.viewMode)

    div(
      cls("h-full flex flex-col"),

      // Header with view mode selector
      div(
        cls(
          "flex items-center justify-between p-4 border-b border-gray-200 bg-gray-50"
        ),
        h3(
          cls("text-lg font-semibold text-gray-800"),
          "Abstract Syntax Tree"
        ),
        div(
          cls("flex gap-2"),
          button(
            cls("px-3 py-1 text-sm rounded transition-colors"),
            cls.toggle("bg-blue-500 text-white") <-- viewModeVar.signal.map(
              _ == ViewMode.Tree
            ),
            cls.toggle(
              "bg-gray-200 text-gray-700 hover:bg-gray-300"
            ) <-- viewModeVar.signal.map(_ != ViewMode.Tree),
            "Tree View",
            onClick --> (_ => viewModeVar.set(ViewMode.Tree))
          ),
          button(
            cls("px-3 py-1 text-sm rounded transition-colors"),
            cls.toggle("bg-blue-500 text-white") <-- viewModeVar.signal.map(
              _ == ViewMode.Json
            ),
            cls.toggle(
              "bg-gray-200 text-gray-700 hover:bg-gray-300"
            ) <-- viewModeVar.signal.map(_ != ViewMode.Json),
            "JSON",
            onClick --> (_ => viewModeVar.set(ViewMode.Json))
          ),
          button(
            cls("px-3 py-1 text-sm rounded transition-colors"),
            cls.toggle("bg-blue-500 text-white") <-- viewModeVar.signal.map(
              _ == ViewMode.Text
            ),
            cls.toggle(
              "bg-gray-200 text-gray-700 hover:bg-gray-300"
            ) <-- viewModeVar.signal.map(_ != ViewMode.Text),
            "Text",
            onClick --> (_ => viewModeVar.set(ViewMode.Text))
          )
        )
      ),

      // Content area
      div(
        cls("flex-1 overflow-auto p-4"),
        child <-- Signal
          .combine(Var(props.parsedFile).signal, viewModeVar.signal)
          .map {
            case (Some(parsedFile), viewMode) =>
              renderContent(parsedFile, viewMode)
            case (None, _) => renderEmpty()
          }
      )
    )
  }

  private def renderEmpty(): HtmlElement = {
    div(
      cls("flex items-center justify-center h-full text-gray-500"),
      div(
        cls("text-center"),
        div(cls("text-4xl mb-4"), "📄"),
        div(cls("text-lg"), "No AST to display"),
        div(cls("text-sm"), "Enter some TypeScript code to see the parsed AST")
      )
    )
  }

  private def renderContent(
      parsedFile: TsParsedFile,
      viewMode: ViewMode
  ): HtmlElement = {
    viewMode match {
      case ViewMode.Tree => renderTreeView(parsedFile)
      case ViewMode.Json => renderJsonView(parsedFile)
      case ViewMode.Text => renderTextView(parsedFile)
    }
  }

  private def renderTreeView(parsedFile: TsParsedFile): HtmlElement = {
    val treeNode = AstFormatter.formatAsTree(parsedFile)

    div(
      cls("font-mono text-sm"),
      renderTreeNode(treeNode, 0)
    )
  }

  private def renderTreeNode(
      node: AstFormatter.TreeNode,
      depth: Int
  ): HtmlElement = {
    val isExpandedVar = Var(depth < 2) // Auto-expand first 2 levels
    val hasChildren = node.children.nonEmpty

    div(
      cls("select-text"),

      // Node header
      div(
        cls("flex items-center py-1 hover:bg-gray-50 rounded tree-node"),
        cls("cursor-pointer") := hasChildren,
        onClick.filter(_ => hasChildren) --> (_ => isExpandedVar.update(!_)),

        // Indentation
        div(cls("w-4") := (depth > 0), " " * (depth * 2)),

        // Expand/collapse icon
        if (hasChildren) {
          span(
            cls("w-4 h-4 flex items-center justify-center text-gray-400 mr-1"),
            child <-- isExpandedVar.signal.map { isExpanded =>
              if (isExpanded) span("▼") else span("▶")
            }
          )
        } else {
          span(cls("w-5 mr-1"))
        },

        // Node type
        span(
          cls("font-semibold text-blue-600"),
          node.nodeType
        ),

        // Node name
        if (node.name.nonEmpty) {
          span(
            cls("text-green-600 ml-1"),
            s"(${node.name})"
          )
        } else emptyNode,

        // Node details
        if (node.details.nonEmpty) {
          span(
            cls("text-gray-600 ml-2 text-xs"),
            s"- ${node.details}"
          )
        } else emptyNode
      ),

      // Children
      if (hasChildren) {
        div(
          cls.toggle("hidden") <-- isExpandedVar.signal.map(!_),
          node.children.map(child => renderTreeNode(child, depth + 1))
        )
      } else emptyNode
    )
  }

  private def renderJsonView(parsedFile: TsParsedFile): HtmlElement = {
    // Simple JSON-like representation
    val jsonContent = formatAsJson(parsedFile)

    pre(
      cls(
        "bg-gray-50 p-4 rounded border text-sm font-mono overflow-auto whitespace-pre-wrap"
      ),
      jsonContent
    )
  }

  private def renderTextView(parsedFile: TsParsedFile): HtmlElement = {
    val treeNode = AstFormatter.formatAsTree(parsedFile)
    val textContent = AstFormatter.renderTreeAsText(treeNode)

    pre(
      cls(
        "bg-gray-50 p-4 rounded border text-sm font-mono overflow-auto whitespace-pre"
      ),
      textContent
    )
  }

  private def formatAsJson(parsedFile: TsParsedFile): String = {
    val treeNode = AstFormatter.formatAsTree(parsedFile)
    formatTreeNodeAsJson(treeNode, 0)
  }

  private def formatTreeNodeAsJson(
      node: AstFormatter.TreeNode,
      indent: Int
  ): String = {
    val prefix = "  " * indent
    val nextPrefix = "  " * (indent + 1)

    val fields = List(
      Some(s"""${nextPrefix}"type": "${node.nodeType}""""),
      if (node.name.nonEmpty)
        Some(s"""${nextPrefix}"name": "${escapeJson(node.name)}"""")
      else None,
      if (node.details.nonEmpty)
        Some(s"""${nextPrefix}"details": "${escapeJson(node.details)}"""")
      else None,
      if (node.children.nonEmpty) {
        val childrenJson = node.children
          .map(child => formatTreeNodeAsJson(child, indent + 2))
          .mkString(",\n")
        Some(s"""${nextPrefix}"children": [\n$childrenJson\n$nextPrefix]""")
      } else None
    ).flatten

    s"$prefix{\n${fields.mkString(",\n")}\n$prefix}"
  }

  private def escapeJson(str: String): String = {
    str
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")
  }
}

object AstDisplay {
  case class Props(
      parsedFile: Option[TsParsedFile],
      viewMode: ViewMode
  )

}
