package www

import org.scalajs.dom
import com.raquo.laminar.api.L.*
import www.components.LaminarComponent
import CodeInput.Examples

case class CodeInput(props: CodeInput.Props) extends LaminarComponent {
  def render(): HtmlElement = {
    val textAreaRef = Var[Option[dom.HTMLTextAreaElement]](None)
    
    div(
      cls("relative"),
      
      // Line numbers
      div(
        cls("absolute left-0 top-0 bottom-0 w-12 bg-gray-50 border-r border-gray-200 text-xs text-gray-500 font-mono leading-6 pt-3 pl-2 select-none"),
        children <-- Var(props.value).signal.map { content =>
          val lineCount = content.split('\n').length
          (1 to lineCount).map { lineNum =>
            div(
              cls("h-6"),
              lineNum.toString
            )
          }
        }
      ),
      
      // Text area
      textArea(
        cls("w-full h-96 pl-14 pr-4 py-3 font-mono text-sm border border-gray-300 rounded-lg resize-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none code-editor"),
        cls.toggle("bg-gray-100 cursor-not-allowed") <-- Var(props.disabled),
        placeholder := props.placeholder,
        disabled <-- Var(props.disabled),
        value <-- Var(props.value),
        onInput.mapToValue --> { newValue =>
          props.onChange(newValue)
        },
        onMountCallback { ctx =>
          textAreaRef.set(Some(ctx.thisNode.ref))
        },
        
        // Handle tab key for indentation
        onKeyDown --> { event =>
          if (event.key == "Tab") {
            event.preventDefault()
            textAreaRef.now().foreach { textarea =>
              val start = textarea.selectionStart
              val end = textarea.selectionEnd
              val value = textarea.value
              
              if (event.shiftKey) {
                // Shift+Tab: Remove indentation
                val beforeCursor = value.substring(0, start)
                val afterCursor = value.substring(end)
                val lines = beforeCursor.split('\n')
                
                if (lines.nonEmpty) {
                  val lastLine = lines.last
                  if (lastLine.startsWith("  ")) {
                    val newLastLine = lastLine.substring(2)
                    val newValue = lines.dropRight(1).mkString("\n") + 
                                  (if (lines.length > 1) "\n" else "") + 
                                  newLastLine + afterCursor
                    textarea.value = newValue
                    textarea.selectionStart = start - 2
                    textarea.selectionEnd = end - 2
                    props.onChange(newValue)
                  }
                }
              } else {
                // Tab: Add indentation
                val newValue = value.substring(0, start) + "  " + value.substring(end)
                textarea.value = newValue
                textarea.selectionStart = start + 2
                textarea.selectionEnd = start + 2
                props.onChange(newValue)
              }
            }
          }
        }
      )
    )
  }
  
  // Predefined examples
  
  
}

object CodeInput {
  case class Props(
    value: String,
    onChange: String => Unit,
    placeholder: String = "Enter TypeScript code here...",
    disabled: Boolean = false
  )

  def exampleSelector(onSelect: String => Unit): HtmlElement = {
    div(
      cls("mb-4"),
      label(
        cls("block text-sm font-medium text-gray-700 mb-2"),
        "Load Example:"
      ),
      div(
        cls("flex flex-wrap gap-2"),
        button(
          cls("px-3 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors"),
          "Simple Interface",
          onClick --> (_ => onSelect(Examples.simple))
        ),
        button(
          cls("px-3 py-1 text-xs bg-green-100 text-green-700 rounded hover:bg-green-200 transition-colors"),
          "Complex Namespace",
          onClick --> (_ => onSelect(Examples.complex))
        ),
        button(
          cls("px-3 py-1 text-xs bg-purple-100 text-purple-700 rounded hover:bg-purple-200 transition-colors"),
          "Function Overloads",
          onClick --> (_ => onSelect(Examples.functions))
        ),
        button(
          cls("px-3 py-1 text-xs bg-orange-100 text-orange-700 rounded hover:bg-orange-200 transition-colors"),
          "Advanced Types",
          onClick --> (_ => onSelect(Examples.types))
        ),
        button(
          cls("px-3 py-1 text-xs bg-gray-100 text-gray-700 rounded hover:bg-gray-200 transition-colors"),
          "Clear",
          onClick --> (_ => onSelect(""))
        )
      )
    )
  }

  object Examples {
    val simple = 
      """interface User {
        |  id: number;
        |  name: string;
        |  email?: string;
        |}""".stripMargin
    
    val complex = 
      """declare namespace MyLibrary {
        |  interface Config {
        |    apiUrl: string;
        |    timeout?: number;
        |    retries?: number;
        |  }
        |  
        |  class ApiClient {
        |    constructor(config: Config);
        |    get<T>(url: string): Promise<T>;
        |    post<T>(url: string, data: any): Promise<T>;
        |  }
        |  
        |  function createClient(config: Config): ApiClient;
        |  
        |  enum LogLevel {
        |    DEBUG = "debug",
        |    INFO = "info",
        |    WARN = "warn",
        |    ERROR = "error"
        |  }
        |  
        |  type EventHandler<T> = (event: T) => void;
        |  
        |  interface EventEmitter<T = any> {
        |    on(event: string, handler: EventHandler<T>): void;
        |    off(event: string, handler: EventHandler<T>): void;
        |    emit(event: string, data: T): void;
        |  }
        |}
        |
        |export = MyLibrary;""".stripMargin
    
    val functions = 
      """declare function overloaded(x: string): string;
        |declare function overloaded(x: number): number;
        |declare function overloaded<T>(x: T[]): T[];
        |
        |declare function withOptional(
        |  required: string,
        |  optional?: number,
        |  callback?: (result: string) => void
        |): Promise<string>;
        |
        |declare function generic<T, U extends keyof T>(
        |  obj: T,
        |  key: U
        |): T[U];""".stripMargin
    
    val types = 
      """type StringOrNumber = string | number;
        |type Partial<T> = {
        |  [P in keyof T]?: T[P];
        |};
        |
        |type EventMap = {
        |  click: MouseEvent;
        |  keydown: KeyboardEvent;
        |  load: Event;
        |};
        |
        |type EventNames = keyof EventMap;
        |type EventHandler<K extends EventNames> = (event: EventMap[K]) => void;
        |
        |interface Component<Props = {}> {
        |  props: Props;
        |  render(): JSX.Element;
        |}
        |
        |type ComponentType<P = {}> = Component<P> | ((props: P) => JSX.Element);""".stripMargin
  }
}